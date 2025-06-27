/**
 * @file KafkaConsumerService.java
 * @description Kafka 메시지 소비 서비스입니다.
 *              Kafka 토픽으로부터 메시지를 수신하고 처리합니다.
 */
package com.nsmm.esg.dart_service.kafka.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nsmm.esg.dart_service.dart.dto.CompanyProfileResponse;
import com.nsmm.esg.dart_service.dart.dto.DisclosureSearchResponse;
import com.nsmm.esg.dart_service.dart.dto.FinancialStatementResponseDto;
import com.nsmm.esg.dart_service.dart.service.DartApiService;
import com.nsmm.esg.dart_service.database.entity.CompanyProfile;
import com.nsmm.esg.dart_service.database.entity.Disclosure;
import com.nsmm.esg.dart_service.database.entity.FinancialStatementData;
import com.nsmm.esg.dart_service.database.repository.CompanyProfileRepository;
import com.nsmm.esg.dart_service.database.repository.DisclosureRepository;
import com.nsmm.esg.dart_service.database.repository.FinancialStatementDataRepository;
import com.nsmm.esg.dart_service.database.repository.PartnerCompanyRepository;
import com.nsmm.esg.dart_service.partner.dto.PartnerCompanyResponseDto;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;
    private final DartApiService dartApiService;
    private final PartnerCompanyRepository partnerCompanyRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final DisclosureRepository disclosureRepository;
    private final FinancialStatementDataRepository financialStatementDataRepository;
    private final KafkaProducerService kafkaProducerService;

    @Value("${dart.api.key}")
    private String dartApiKey;

    private static final String FS_DIV_OFS = "OFS";
    private static final String[] REPORT_CODES_ANNUAL_QUARTERLY = { "11011", "11012", "11013", "11014" };

    /**
     * 회사 정보 토픽에서 메시지를 소비합니다.
     *
     * @param message 수신된 메시지
     */
    @KafkaListener(topics = "${kafka.topic.company-profile}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeCompanyProfile(String message) {
        log.info("회사 정보 메시지 수신: {}", message);
        try {
            // 실제 구현에서는 메시지를 처리하는 로직 추가
            log.info("회사 정보 메시지 처리 완료");
        } catch (Exception e) {
            log.error("회사 정보 메시지 처리 중 오류 발생", e);
        }
    }

    /**
     * 공시 정보 토픽에서 메시지를 소비합니다.
     *
     * @param message 수신된 메시지
     */
    @KafkaListener(topics = "${kafka.topic.disclosure}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeDisclosure(String message) {
        log.info("공시 정보 메시지 수신: {}", message);
        try {
            // 실제 구현에서는 메시지를 처리하는 로직 추가
            log.info("공시 정보 메시지 처리 완료");
        } catch (Exception e) {
            log.error("공시 정보 메시지 처리 중 오류 발생", e);
        }
    }

    /**
     * 파트너 회사 토픽에서 메시지를 소비합니다.
     *
     * @param partnerCompanyDto 수신된 메시지
     */
    @KafkaListener(topics = "${kafka.topic.partner-company}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumePartnerCompany(PartnerCompanyResponseDto partnerCompanyDto) {
        log.info("파트너 회사 DTO 메시지 수신: {}", partnerCompanyDto);
        try {
            log.info("파트너 회사 메시지 자동 변환 완료: ID={}, 회사명={}, 고유번호={}",
                    partnerCompanyDto.getId(), partnerCompanyDto.getCorpName(), partnerCompanyDto.getCorpCode());

            partnerCompanyRepository.findById(partnerCompanyDto.getId()).ifPresentOrElse(
                    partner -> log.info("DB에서 파트너사 확인: ID={}, 이름={}", partner.getId(), partner.getCompanyName()),
                    () -> log.warn("파트너사 ID {} 에 해당하는 정보가 DB에 없습니다. (Kafka 메시지 기준 처리 계속)", partnerCompanyDto.getId()));

            if (partnerCompanyDto.getCorpCode() != null && !partnerCompanyDto.getCorpCode().isEmpty()) {
                String corpCode = partnerCompanyDto.getCorpCode();
                log.info("DART 연동 시작: corpCode={}", corpCode);

                CompanyProfile companyProfile = saveOrUpdateCompanyProfileByCorpCode(corpCode);

                if (companyProfile != null) {
                    retrieveAndSaveDisclosures(corpCode, companyProfile);

                    retrieveAndSaveRecentFinancialStatements(corpCode);

                    log.info("파트너사 DART 연동 완료: corpCode={}", corpCode);
                } else {
                    log.warn("회사 프로필 정보를 가져오거나 생성할 수 없어 DART 연동 중단: corpCode={}", corpCode);
                }
            } else {
                log.warn("파트너사 메시지에 corpCode가 없어 DART 연동을 수행할 수 없습니다: ID={}", partnerCompanyDto.getId());

                log.info("파트너사 등록 완료 (DART 연동 제외): ID={}", partnerCompanyDto.getId());
            }

            log.info("파트너 회사 메시지 처리 완료: ID={}", partnerCompanyDto.getId());
        } catch (Exception e) {
            log.error("파트너 회사 메시지 처리 중 오류 발생: ID={}", partnerCompanyDto.getId(), e);
        }
    }

    private CompanyProfile saveOrUpdateCompanyProfileByCorpCode(String corpCode) {
        try {
            // 먼저 DB에서 회사 프로필이 이미 존재하는지 확인
            Optional<CompanyProfile> existingProfile = companyProfileRepository.findById(corpCode);
            if (existingProfile.isPresent()) {
                log.info("DB에서 기존 회사 프로필 정보 발견: corpCode={}, corpName={}",
                        corpCode, existingProfile.get().getCorpName());
                return existingProfile.get();
            }

            log.info("DART API를 통해 회사 정보 조회 시도 (transform 사용): corpCode={}", corpCode);

            java.util.function.Function<Mono<CompanyProfileResponse>, Mono<CompanyProfile>> processApiResponse = apiResponseMono -> apiResponseMono
                    .flatMap(profileResponse -> {
                        if ("000".equals(profileResponse.getStatus())) {
                            log.info("DART API 성공 (transform): {}", profileResponse.getCorpName());
                            return Mono.just(saveOrUpdateCompanyProfile(profileResponse));
                        } else {
                            log.warn(
                                    "DART API 오류 또는 데이터 없음 (transform - 응답은 받았으나 status 불일치): corpCode={}, status={}, message={}",
                                    corpCode, profileResponse.getStatus(), profileResponse.getMessage());
                            return Mono.<CompanyProfile>empty();
                        }
                    })
                    .switchIfEmpty(Mono.fromSupplier(() -> {
                        log.warn("DART API 응답이 비어있음 (transform - switchIfEmpty): corpCode={}", corpCode);
                        return null;
                    }))
                    .onErrorResume(e -> {
                        log.error("DART API 처리 중 예외 발생 (transform - onErrorResume): corpCode={}", corpCode, e);
                        return Mono.<CompanyProfile>empty();
                    });

            Optional<CompanyProfile> profileOptional = dartApiService.getCompanyProfile(corpCode)
                    .transform(processApiResponse)
                    .blockOptional();

            if (profileOptional.isPresent()) {
                return profileOptional.get();
            } else {
                // DART API에서 정보를 가져오지 못한 경우, 기본 프로필 생성
                log.info("DART API에서 정보를 가져오지 못해 기본 회사 프로필 생성: corpCode={}", corpCode);
                LocalDateTime now = LocalDateTime.now();
                CompanyProfile defaultProfile = CompanyProfile.builder()
                        .corpCode(corpCode)
                        .corpName("기본 회사명_" + corpCode) // 기본 회사명 (필수 필드)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                return companyProfileRepository.save(defaultProfile);
            }

        } catch (Exception e) {
            log.error("saveOrUpdateCompanyProfileByCorpCode 메소드 실행 중 예상치 못한 예외 발생: corpCode={}", corpCode, e);
            return null;
        }
    }

    private CompanyProfile saveOrUpdateCompanyProfile(CompanyProfileResponse profileResponse) {
        log.info("회사 프로필 정보 저장/업데이트: corpCode={}, corpName={}",
                profileResponse.getCorpCode(), profileResponse.getCorpName());

        Optional<CompanyProfile> existingProfile = companyProfileRepository.findById(profileResponse.getCorpCode());
        CompanyProfile companyProfile;
        if (existingProfile.isPresent()) {
            companyProfile = existingProfile.get();
            updateCompanyProfile(companyProfile, profileResponse);
        } else {
            companyProfile = createCompanyProfile(profileResponse);
        }
        return companyProfileRepository.save(companyProfile);
    }

    private void updateCompanyProfile(CompanyProfile companyProfile, CompanyProfileResponse profileResponse) {
        companyProfile.setCorpName(profileResponse.getCorpName());
        companyProfile.setCorpNameEng(profileResponse.getCorpNameEng());
        companyProfile.setStockCode(profileResponse.getStockCode());
        companyProfile.setCeoName(profileResponse.getCeoName());
        companyProfile.setCorpClass(profileResponse.getCorpClass());
        companyProfile.setBusinessNumber(profileResponse.getBusinessNumber());
        companyProfile.setCorporateRegistrationNumber(profileResponse.getCorporateRegistrationNumber());
        companyProfile.setAddress(profileResponse.getAddress());
        companyProfile.setHomepageUrl(profileResponse.getHomepageUrl());
        companyProfile.setIrUrl(profileResponse.getIrUrl());
        companyProfile.setPhoneNumber(profileResponse.getPhoneNumber());
        companyProfile.setFaxNumber(profileResponse.getFaxNumber());
        companyProfile.setIndustry(profileResponse.getIndustry());
        companyProfile.setEstablishmentDate(profileResponse.getEstablishmentDate());
        companyProfile.setAccountingMonth(profileResponse.getAccountingMonth());
        companyProfile.setUpdatedAt(LocalDateTime.now());
    }

    private CompanyProfile createCompanyProfile(CompanyProfileResponse profileResponse) {
        LocalDateTime now = LocalDateTime.now();
        return CompanyProfile.builder()
                .corpCode(profileResponse.getCorpCode())
                .corpName(profileResponse.getCorpName())
                .corpNameEng(profileResponse.getCorpNameEng())
                .stockCode(profileResponse.getStockCode())
                .ceoName(profileResponse.getCeoName())
                .corpClass(profileResponse.getCorpClass())
                .businessNumber(profileResponse.getBusinessNumber())
                .corporateRegistrationNumber(profileResponse.getCorporateRegistrationNumber())
                .address(profileResponse.getAddress())
                .homepageUrl(profileResponse.getHomepageUrl())
                .irUrl(profileResponse.getIrUrl())
                .phoneNumber(profileResponse.getPhoneNumber())
                .faxNumber(profileResponse.getFaxNumber())
                .industry(profileResponse.getIndustry())
                .establishmentDate(profileResponse.getEstablishmentDate())
                .accountingMonth(profileResponse.getAccountingMonth())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void retrieveAndSaveDisclosures(String corpCode, CompanyProfile companyProfile) {
        log.info("회사의 공시 정보 조회 및 저장: corpCode={}", corpCode);
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusYears(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String startDateStr = startDate.format(formatter);
            String endDateStr = endDate.format(formatter);
            log.info("공시 정보 조회 기간: {} ~ {}", startDateStr, endDateStr);
            DisclosureSearchResponse disclosureResponse = dartApiService
                    .searchDisclosures(corpCode, startDateStr, endDateStr).block();
            if (disclosureResponse != null && disclosureResponse.getList() != null
                    && !disclosureResponse.getList().isEmpty()) {
                log.info("공시 정보 조회 성공: {} 건", disclosureResponse.getList().size());
                for (DisclosureSearchResponse.DisclosureItem item : disclosureResponse.getList()) {
                    saveDisclosure(item, companyProfile);
                }
                log.info("공시 정보 저장 완료: corpCode={}, 건수={}",
                        corpCode, disclosureResponse.getList().size());
            } else {
                log.info("조회된 공시 정보가 없습니다: corpCode={}", corpCode);
            }
        } catch (Exception e) {
            log.error("공시 정보 조회 및 저장 중 오류 발생: corpCode={}", corpCode, e);
        }
    }

    private void saveDisclosure(DisclosureSearchResponse.DisclosureItem item, CompanyProfile companyProfile) {
        if (disclosureRepository.existsById(item.getReceiptNo())) {
            log.debug("이미 존재하는 공시 정보입니다: receiptNo={}", item.getReceiptNo());
            return;
        }
        try {
            LocalDate receiptDate = LocalDate.parse(item.getReceiptDate(), DateTimeFormatter.ofPattern("yyyyMMdd"));
            Disclosure disclosure = Disclosure.builder()
                    .receiptNo(item.getReceiptNo())
                    .companyProfile(companyProfile)
                    .corpName(item.getCorpName())
                    .stockCode(item.getStockCode())
                    .corpClass(item.getCorpClass())
                    .reportName(item.getReportName())
                    .submitterName(item.getSubmitterName())
                    .receiptDate(receiptDate)
                    .remark(item.getRemark())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            disclosureRepository.save(disclosure);
            log.debug("공시 정보 저장 완료: receiptNo={}, reportName={}", item.getReceiptNo(), item.getReportName());
        } catch (Exception e) {
            log.error("공시 정보 저장 중 오류 발생: receiptNo={}", item.getReceiptNo(), e);
        }
    }

    /**
     * 특정 회사의 최근 1~2년치 주요 재무제표를 조회하고 DB에 저장합니다.
     * - 작년도: 사업보고서 (11011)
     * - 올해: 1분기(11013), 반기(11012), 3분기(11014) 보고서 (존재하는 경우)
     * 
     * @param corpCode 회사 고유번호
     */
    private void retrieveAndSaveRecentFinancialStatements(String corpCode) {
        log.info("최근 1~2년치 재무제표 조회 및 저장 시작: corpCode={}", corpCode);
        LocalDate today = LocalDate.now();
        String currentYear = String.valueOf(today.getYear());
        String lastYear = String.valueOf(today.minusYears(1).getYear());

        retrieveAndSaveSingleFinancialStatement(corpCode, lastYear, "11011");

        retrieveAndSaveSingleFinancialStatement(corpCode, currentYear, "11014");
        retrieveAndSaveSingleFinancialStatement(corpCode, currentYear, "11012");
        retrieveAndSaveSingleFinancialStatement(corpCode, currentYear, "11013");
    }

    /**
     * 특정 연도, 특정 보고서 코드에 대한 재무제표를 조회하고 저장합니다.
     * 
     * @param corpCode  회사 고유번호
     * @param bsnsYear  사업연도 (YYYY)
     * @param reprtCode 보고서 코드
     */
    private void retrieveAndSaveSingleFinancialStatement(String corpCode, String bsnsYear, String reprtCode) {
        log.info("단일 재무제표 조회 및 저장 시도: corpCode={}, bsnsYear={}, reprtCode={}, fsDiv={}",
                corpCode, bsnsYear, reprtCode, FS_DIV_OFS);
        try {
            long deletedCount = financialStatementDataRepository.deleteByCorpCodeAndBsnsYearAndReprtCode(corpCode,
                    bsnsYear, reprtCode);
            if (deletedCount > 0) {
                log.info("기존 재무제표 데이터 {}건 삭제: corpCode={}, bsnsYear={}, reprtCode={}",
                        deletedCount, corpCode, bsnsYear, reprtCode);
            }

            FinancialStatementResponseDto responseDto = dartApiService
                    .getFinancialStatement(corpCode, bsnsYear, reprtCode, FS_DIV_OFS).block();

            if (responseDto != null && "000".equals(responseDto.getStatus()) && responseDto.getList() != null
                    && !responseDto.getList().isEmpty()) {
                log.info("재무제표 조회 성공: {}건의 항목. corpCode={}, bsnsYear={}, reprtCode={}",
                        responseDto.getList().size(), corpCode, bsnsYear, reprtCode);
                processAndSaveFinancialStatementItems(responseDto.getList(), corpCode, bsnsYear, reprtCode);
            } else {
                log.warn("재무제표 데이터가 없거나 오류 발생: corpCode={}, bsnsYear={}, reprtCode={}, status={}, msg={}",
                        corpCode, bsnsYear, reprtCode,
                        responseDto != null ? responseDto.getStatus() : "N/A",
                        responseDto != null ? responseDto.getMessage() : "Response is null or empty list");
            }
        } catch (Exception e) {
            log.error("재무제표 조회/저장 중 예외 발생: corpCode={}, bsnsYear={}, reprtCode={}", corpCode, bsnsYear, reprtCode, e);
        }
    }

    private void processAndSaveFinancialStatementItems(List<FinancialStatementResponseDto.FinancialStatementItem> items,
            String corpCode, String bsnsYear, String reprtCode) {
        List<FinancialStatementData> dataToSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (FinancialStatementResponseDto.FinancialStatementItem item : items) {
            FinancialStatementData fsData = FinancialStatementData.builder()
                    .corpCode(corpCode)
                    .bsnsYear(bsnsYear)
                    .reprtCode(reprtCode)
                    .sjDiv(item.getSjDiv())
                    .accountId(item.getAccountId())
                    .accountNm(item.getAccountNm())
                    .thstrmNm(item.getThstrmNm())
                    .thstrmAmount(item.getThstrmAmount())
                    .thstrmAddAmount(item.getThstrmAddAmount())
                    .frmtrmNm(item.getFrmtrmNm())
                    .frmtrmAmount(item.getFrmtrmAmount())
                    .frmtrmQNm(item.getFrmtrmQNm())
                    .frmtrmQAmount(item.getFrmtrmQAmount())
                    .frmtrmAddAmount(item.getFrmtrmAddAmount())
                    .bfefrmtrmNm(item.getBfefrmtrmNm())
                    .bfefrmtrmAmount(item.getBfefrmtrmAmount())
                    .currency(item.getCurrency())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            dataToSave.add(fsData);
        }

        if (!dataToSave.isEmpty()) {
            financialStatementDataRepository.saveAll(dataToSave);
            log.info("DB에 재무제표 항목 {}건 저장 완료: corpCode={}, bsnsYear={}, reprtCode={}",
                    dataToSave.size(), corpCode, bsnsYear, reprtCode);
        } else {
            log.info("DB에 저장할 재무제표 항목 없음: corpCode={}, bsnsYear={}, reprtCode={}",
                    corpCode, bsnsYear, reprtCode);
        }
    }
}
