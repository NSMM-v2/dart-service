/**
 * @file DartApiService.java
 * @description DART Open API와 통신하기 위한 서비스 클래스입니다.
 *              회사 정보, 공시 정보 등 DART API의 다양한 기능을 호출합니다.
 *              복잡도를 줄이기 위해 리팩토링되었습니다.
 */
package com.nsmm.esg.dart_service.dart.service;

import com.nsmm.esg.dart_service.dart.dto.CompanyProfileResponse;
import com.nsmm.esg.dart_service.dart.dto.CorpCodeQueryDto;
import com.nsmm.esg.dart_service.dart.dto.DisclosureSearchResponse;
import com.nsmm.esg.dart_service.dart.dto.DartCorpCodeRootXmlDto;
import com.nsmm.esg.dart_service.dart.dto.FinancialStatementResponseDto;
import com.nsmm.esg.dart_service.dart.dto.DartCorpCodeXmlDto;
import com.nsmm.esg.dart_service.dart.service.XmlParserService.ParseResult;
import com.nsmm.esg.dart_service.database.entity.DartCorpCode;
import com.nsmm.esg.dart_service.database.repository.DartCorpCodeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DartApiService {

    private final DartCorpCodeRepository dartCorpCodeRepository;
    private final WebClientService webClientService;
    private final XmlParserService xmlParserService;
    private final ZipExtractorService zipExtractorService;

    private static final String CORP_CODE_CACHE_NAME = "dartCorpCodes";

    @PostConstruct
    public void init() {
        // 모듈 초기화 시 기업 코드 동기화 (DB가 비어있을 경우)
        try {
            synchronizeCorpCodesIfNeeded();
        } catch (Exception e) {
            log.error("애플리케이션 초기화 중 기업 코드 동기화 실패. 애플리케이션은 계속 실행됩니다.", e);
        }
    }

    private void synchronizeCorpCodesIfNeeded() {
        long count = dartCorpCodeRepository.count();
        if (count == 0) {
            log.info("데이터베이스에 기업 코드가 없습니다. 초기 동기화를 시도합니다...");
            fetchAndStoreCorpCodes()
                    .doOnError(error -> log.error("초기 기업 코드 동기화 중 오류 발생", error))
                    .subscribe(
                            null, // onNext (결과 무시)
                            error -> {
                            }, // onError는 doOnError에서 이미 처리
                            () -> log.info("초기 기업 코드 동기화 시도 완료."));
        }
    }

    // 매일 새벽 4시에 기업 코드 동기화
    @Scheduled(cron = "0 0 4 * * *")
    @CacheEvict(value = CORP_CODE_CACHE_NAME, allEntries = true)
    public void scheduledSyncCorpCodes() {
        log.info("스케줄에 따른 기업 코드 동기화 시작...");
        fetchAndStoreCorpCodes()
                .doOnError(error -> log.error("스케줄된 기업 코드 동기화 중 오류 발생", error))
                .subscribe(
                        null,
                        error -> {
                        },
                        () -> log.info("스케줄된 기업 코드 동기화 완료."));
    }

    /**
     * DART API에서 기업 코드를 가져와 데이터베이스에 저장합니다.
     * 이 메서드는 복잡한 로직을 여러 서비스로 분리하여 단순화되었습니다.
     *
     * @return 완료 시그널
     */
    @Transactional
    public Mono<Void> fetchAndStoreCorpCodes() {
        long existingCount = dartCorpCodeRepository.count();
        if (existingCount > 0) {
            log.info("기업 코드가 이미 DB에 존재함 (총 {}건) - 다운로드 및 저장 생략", existingCount);
            return Mono.empty(); // 이미 존재하면 무시
        }

        log.info("DART API로부터 기업 코드 다운로드 및 저장을 시작합니다...");

        return webClientService.downloadCorpCodeZip()
                .transform(zipExtractorService::extractXmlFromZip)
                .map(xmlParserService::parseCorpCodeXml)
                .flatMap(this::processCorpCodeParseResult)
                .doOnError(error -> log.error("기업 코드 처리 중 오류 발생: {}", error.getMessage(), error))
                .then();
    }

    /**
     * XML 파싱 결과를 처리하여 기업 코드를 데이터베이스에 저장합니다.
     *
     * @param parseResult XML 파싱 결과
     * @return 완료 시그널
     */
    private Mono<Void> processCorpCodeParseResult(ParseResult parseResult) {
        String status = parseResult.getStatus();
        String message = parseResult.getMessage();
        List<?> corpList = parseResult.getCorpList();

        // 상태가 null이 아니고 "000"이 아닌 경우에만 오류로 처리
        // 상태가 null이지만 목록이 있는 경우는 정상으로 간주하고 계속 진행
        if (status != null && !"000".equals(status)) {
            log.error("DART API 오류 응답: status={}, message={}", status, message);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "DART API error: " + message));
        }

        if (corpList == null || corpList.isEmpty()) {
            log.warn("DART API에서 받은 기업 코드 목록이 비어있습니다.");
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "DART API returned empty corp code list"));
        }

        log.info("DART API에서 {}개의 기업 코드를 받았습니다. 데이터베이스에 저장합니다...", corpList.size());

        // 기업 코드 목록을 데이터베이스에 저장
        List<DartCorpCode> dartCorpCodes = new ArrayList<>();

        for (Object item : corpList) {
            try {
                DartCorpCode dartCorpCode = new DartCorpCode();
                LocalDateTime now = LocalDateTime.now();

                if (item instanceof DartCorpCodeXmlDto.CorpCodeItem corpItem) {
                    // Handle CorpCodeItem objects

                    dartCorpCode.setCorpCode(corpItem.getCorpCode());
                    dartCorpCode.setCorpName(corpItem.getCorpName());
                    dartCorpCode.setCorpEngName(corpItem.getCorpEngName()); // 영문 회사명 추가
                    dartCorpCode.setStockCode(corpItem.getStockCode());
                    dartCorpCode.setModifyDate(corpItem.getModifyDate());
                } else if (item instanceof DartCorpCodeRootXmlDto.CorpCodeItem corpItem) {
                    // Handle CorpCodeItem objects from alternative DTO

                    dartCorpCode.setCorpCode(corpItem.getCorpCode());
                    dartCorpCode.setCorpName(corpItem.getCorpName());
                    dartCorpCode.setCorpEngName(corpItem.getCorpEngName()); // 영문 회사명 추가
                    dartCorpCode.setStockCode(corpItem.getStockCode());
                    dartCorpCode.setModifyDate(corpItem.getModifyDate());
                } else {
                    log.warn("기업 코드 항목 처리 중 오류 발생: 지원되지 않는 항목 유형 - {}", item.getClass().getName());
                    continue; // Skip this item
                }

                dartCorpCode.setCreatedAt(now);
                dartCorpCode.setUpdatedAt(now);
                dartCorpCodes.add(dartCorpCode);
            } catch (Exception e) {
                log.warn("기업 코드 항목 처리 중 오류 발생: {}", e.getMessage());
                // 개별 항목 오류는 무시하고 계속 진행
            }
        }

        if (dartCorpCodes.isEmpty()) {
            log.error("처리 가능한 기업 코드가 없습니다.");
            return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No valid corp codes to save"));
        }

        // 기존 데이터 삭제 후 새 데이터 저장
        dartCorpCodeRepository.deleteAll();
        dartCorpCodeRepository.saveAll(dartCorpCodes);

        log.info("{}개의 기업 코드를 데이터베이스에 저장했습니다.", dartCorpCodes.size());
        return Mono.empty();
    }

    @Cacheable(value = CORP_CODE_CACHE_NAME, key = "#queryDto.toString()")
    public Page<DartCorpCode> getAllCorpCodes(CorpCodeQueryDto queryDto) {
        log.info("저장된 기업 코드 조회 요청: {}", queryDto);

        // 페이지 번호 검증 및 보정
        int validPage = Math.max(0, queryDto.getPage());
        int validPageSize = Math.max(1, Math.min(100, queryDto.getPageSize()));

        log.debug("DART 기업 코드 조회 - 원본: page={}, pageSize={} -> 보정: page={}, pageSize={}",
                queryDto.getPage(), queryDto.getPageSize(), validPage, validPageSize);

        Pageable pageable = PageRequest.of(validPage, validPageSize);

        Specification<DartCorpCode> spec = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(queryDto.getCorpNameFilter())) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("corpName")),
                        "%" + queryDto.getCorpNameFilter().toLowerCase() + "%"));
            }
            if (queryDto.isListedOnly()) {
                predicates.add(criteriaBuilder.isNotNull(root.get("stockCode")));
                predicates.add(criteriaBuilder.notEqual(root.get("stockCode"), ""));
            }
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return dartCorpCodeRepository.findAll(spec, pageable);
    }

    @Cacheable(value = CORP_CODE_CACHE_NAME, key = "#corpCode")
    public Optional<DartCorpCode> findCorpCodeByCorpCode(String corpCode) {
        log.info("기업 코드로 조회: {}", corpCode);
        return dartCorpCodeRepository.findById(corpCode);
    }

    @Cacheable(value = CORP_CODE_CACHE_NAME, key = "'stockCode:' + #stockCode")
    public Optional<DartCorpCode> findCorpCodeByStockCode(String stockCode) {
        log.info("종목 코드로 조회: {}", stockCode);
        return dartCorpCodeRepository.findByStockCode(stockCode);
    }

    @Cacheable(value = CORP_CODE_CACHE_NAME, key = "'name:' + #corpName")
    public List<DartCorpCode> searchCorpCodesByName(String corpName) {
        log.info("회사명으로 검색: {}", corpName);
        return dartCorpCodeRepository.findByCorpNameContainingIgnoreCase(corpName);
    }

    /**
     * DART API에서 회사 정보를 조회합니다.
     * CacheService를 사용하여 프로그래밍 방식으로 캐싱합니다.
     *
     * @param corpCode 회사 코드
     * @return 회사 정보 응답 Mono
     */
    public Mono<CompanyProfileResponse> getCompanyProfile(String corpCode) {
        log.info("회사 정보 조회 API 호출 (캐시 우회 - 디버깅용): {}", corpCode);

        // 캐시를 우회하고 직접 API 호출 (디버깅용)
        log.info("캐시 우회하고 DART API 직접 호출: {}", corpCode);
        return webClientService.getCompanyProfile(corpCode)
                .doOnSuccess(profile -> {
                    if (profile != null) {
                        log.info("DART API 호출 성공: corpCode={}, status={}, corpName={}",
                                corpCode, profile.getStatus(), profile.getCorpName());
                    } else {
                        log.warn("DART API 호출 결과가 null: corpCode={}", corpCode);
                    }
                })
                .doOnError(error -> log.error("회사 정보 조회 중 오류 발생: corpCode={}", corpCode, error));
    }

    /**
     * DART API에서 공시 정보를 검색합니다.
     *
     * @param corpCode  회사 코드
     * @param startDate 검색 시작일(YYYYMMDD)
     * @param endDate   검색 종료일(YYYYMMDD)
     * @return 공시 검색 결과 응답 Mono
     */
    @Cacheable(value = "disclosureSearch", key = "#corpCode + '_' + #startDate + '_' + #endDate")
    public Mono<DisclosureSearchResponse> searchDisclosures(String corpCode, String startDate, String endDate) {
        log.info("공시 검색 API 호출: {}, {} ~ {}", corpCode, startDate, endDate);
        return webClientService.searchDisclosures(corpCode, startDate, endDate);
    }

    /**
     * DART API에서 단일 회사 전체 재무제표를 조회합니다.
     *
     * @param corpCode  고유번호 (8자리)
     * @param bsnsYear  사업연도 (4자리)
     * @param reprtCode 보고서 코드 (1분기: 11013, 반기: 11012, 3분기: 11014, 사업보고서: 11011)
     * @param fsDiv     개별/연결 구분 (OFS: 재무제표, CFS: 연결재무제표)
     * @return 재무제표 정보 응답 Mono
     */
    @Cacheable(value = "financialStatements", key = "#corpCode + '_' + #bsnsYear + '_' + #reprtCode + '_' + #fsDiv")
    public Mono<FinancialStatementResponseDto> getFinancialStatement(String corpCode, String bsnsYear, String reprtCode,
            String fsDiv) {
        log.info("단일 회사 전체 재무제표 조회 API 호출: corpCode={}, bsnsYear={}, reprtCode={}, fsDiv={}",
                corpCode, bsnsYear, reprtCode, fsDiv);
        return webClientService.getFinancialStatementApi(corpCode, bsnsYear, reprtCode, fsDiv);
    }
}
