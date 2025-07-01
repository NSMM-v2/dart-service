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
import com.nsmm.esg.dart_service.database.entity.DartCorpCode;
import com.nsmm.esg.dart_service.database.entity.Disclosure;
import com.nsmm.esg.dart_service.database.entity.FinancialStatementData;
import com.nsmm.esg.dart_service.database.repository.CompanyProfileRepository;
import com.nsmm.esg.dart_service.database.repository.DartCorpCodeRepository;
import com.nsmm.esg.dart_service.database.repository.DisclosureRepository;
import com.nsmm.esg.dart_service.database.repository.FinancialStatementDataRepository;
import com.nsmm.esg.dart_service.partner.dto.PartnerCompanyKafkaMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final DartApiService dartApiService;
    private final CompanyProfileRepository companyProfileRepository;
    private final DisclosureRepository disclosureRepository;
    private final FinancialStatementDataRepository financialStatementDataRepository;
    private final DartCorpCodeRepository dartCorpCodeRepository;

    @Value("${dart.api.key}")
    private String dartApiKey;

    private static final String FS_DIV_OFS = "OFS";

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
     * @param kafkaMessage 수신된 Kafka 메시지
     */
    @KafkaListener(topics = "${kafka.topic.partner-company}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumePartnerCompany(PartnerCompanyKafkaMessage kafkaMessage) {
        log.info("파트너 회사 Kafka 메시지 수신: {}", kafkaMessage);
        try {
            log.info("파트너 회사 메시지 처리 시작: corpCode={}, action={}, timestamp={}",
                    kafkaMessage.getCorpCode(), kafkaMessage.getAction(), kafkaMessage.getTimestamp());

            if (kafkaMessage.getCorpCode() != null && !kafkaMessage.getCorpCode().isEmpty()) {
                String corpCode = kafkaMessage.getCorpCode();
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
                log.warn("Kafka 메시지에 corpCode가 없어 DART 연동을 수행할 수 없습니다: action={}", kafkaMessage.getAction());
                log.info("파트너사 이벤트 처리 완료 (DART 연동 제외): action={}", kafkaMessage.getAction());
            }

            log.info("파트너 회사 Kafka 메시지 처리 완료: corpCode={}, action={}",
                    kafkaMessage.getCorpCode(), kafkaMessage.getAction());
        } catch (Exception e) {
            log.error("파트너 회사 Kafka 메시지 처리 중 오류 발생: corpCode={}, action={}",
                    kafkaMessage.getCorpCode(), kafkaMessage.getAction(), e);
        }
    }

    private CompanyProfile saveOrUpdateCompanyProfileByCorpCode(String corpCode) {
        try {
            // 먼저 DB에서 회사 프로필이 이미 존재하는지 확인 (중복 처리)
            List<CompanyProfile> existingProfiles = companyProfileRepository.findAllByCorpCode(corpCode);

            if (!existingProfiles.isEmpty()) {
                // 중복 데이터가 있는 경우 처리
                if (existingProfiles.size() > 1) {
                    log.warn("CompanyProfile 중복 데이터 발견: corpCode={}, 개수={}", corpCode, existingProfiles.size());
                    CompanyProfile bestProfile = consolidateCompanyProfiles(existingProfiles, corpCode);
                    return bestProfile;
                }

                CompanyProfile profile = existingProfiles.get(0);
                log.info("DB에서 기존 회사 프로필 정보 발견: corpCode={}, corpName={}",
                        corpCode, profile.getCorpName());

                // 기존 프로필에 상세 정보가 없으면 DART API 호출해서 업데이트
                if (needsDetailUpdate(profile)) {
                    log.info("기존 프로필에 상세 정보 부족, DART API 호출하여 업데이트: corpCode={}", corpCode);
                    return updateProfileWithDartApi(profile, corpCode);
                }

                return profile;
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
                log.warn("DART API에서 정보를 가져오지 못해 기본 회사 프로필 생성: corpCode={}", corpCode);

                // DartCorpCode에서 회사명 정보 조회 시도
                String companyName = "정보 없음"; // 기본값
                try {
                    Optional<DartCorpCode> dartCorpCodeOpt = dartCorpCodeRepository.findById(corpCode);
                    if (dartCorpCodeOpt.isPresent()) {
                        companyName = dartCorpCodeOpt.get().getCorpName();
                        log.info("DartCorpCode에서 회사명 조회 성공: corpCode={}, corpName={}", corpCode, companyName);
                    } else {
                        log.warn("DartCorpCode에서도 회사명을 찾을 수 없음: corpCode={}", corpCode);
                    }
                } catch (Exception e) {
                    log.error("DartCorpCode 조회 중 오류 발생: corpCode={}", corpCode, e);
                }

                LocalDateTime now = LocalDateTime.now();
                CompanyProfile defaultProfile = CompanyProfile.builder()
                        .corpCode(corpCode)
                        .corpName(companyName) // DartCorpCode에서 가져온 회사명 또는 "정보 없음"
                        .headquartersId(null) // 소유자 미정 (DART API 업데이트용 임시 프로필)
                        .partnerId(null)
                        .userType("UNKNOWN") // 소유자 미정 상태
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

    /**
     * CompanyProfile에 상세 정보 업데이트가 필요한지 확인
     */
    private boolean needsDetailUpdate(CompanyProfile profile) {
        // 기본적인 상세 정보들이 null이거나 비어있으면 업데이트 필요
        return profile.getCeoName() == null ||
                profile.getAddress() == null ||
                profile.getPhoneNumber() == null ||
                profile.getBusinessNumber() == null ||
                profile.getIndustryCode() == null;
    }

    /**
     * 기존 CompanyProfile을 DART API로 업데이트
     */
    private CompanyProfile updateProfileWithDartApi(CompanyProfile existingProfile, String corpCode) {
        try {
            log.info("기존 프로필 DART API 업데이트 시작: corpCode={}", corpCode);

            Optional<CompanyProfile> updatedProfileOpt = dartApiService.getCompanyProfile(corpCode)
                    .flatMap(profileResponse -> {
                        if ("000".equals(profileResponse.getStatus())) {
                            log.info("DART API 업데이트 성공: corpCode={}, corpName={}",
                                    corpCode, profileResponse.getCorpName());
                            updateCompanyProfile(existingProfile, profileResponse);
                            return Mono.just(companyProfileRepository.save(existingProfile));
                        } else {
                            log.warn("DART API 업데이트 실패: corpCode={}, status={}, message={}",
                                    corpCode, profileResponse.getStatus(), profileResponse.getMessage());
                            return Mono.just(existingProfile); // 기존 프로필 그대로 반환
                        }
                    })
                    .onErrorResume(e -> {
                        log.error("DART API 업데이트 중 오류 발생: corpCode={}", corpCode, e);
                        return Mono.just(existingProfile); // 기존 프로필 그대로 반환
                    })
                    .blockOptional();

            return updatedProfileOpt.orElse(existingProfile);
        } catch (Exception e) {
            log.error("기존 프로필 DART API 업데이트 중 예외 발생: corpCode={}", corpCode, e);
            return existingProfile; // 기존 프로필 그대로 반환
        }
    }

    private CompanyProfile saveOrUpdateCompanyProfile(CompanyProfileResponse profileResponse) {
        log.info("회사 프로필 정보 저장/업데이트: corpCode={}, corpName={}",
                profileResponse.getCorpCode(), profileResponse.getCorpName());

        Optional<CompanyProfile> existingProfile = companyProfileRepository
                .findByCorpCode(profileResponse.getCorpCode());
        CompanyProfile companyProfile;
        if (existingProfile.isPresent()) {
            companyProfile = existingProfile.get();
            updateCompanyProfile(companyProfile, profileResponse);
        } else {
            companyProfile = createCompanyProfile(profileResponse);
        }

        // CompanyProfile 저장
        CompanyProfile savedProfile = companyProfileRepository.save(companyProfile);

        return savedProfile;
    }

    private void updateCompanyProfile(CompanyProfile companyProfile, CompanyProfileResponse profileResponse) {
        companyProfile.setCorpName(profileResponse.getCorpName());
        companyProfile.setCorpNameEng(profileResponse.getCorpNameEng());
        companyProfile.setStockCode(profileResponse.getStockCode());
        companyProfile.setStockName(profileResponse.getStockName()); // 종목명 추가
        companyProfile.setCeoName(profileResponse.getCeoName());
        companyProfile.setCorpClass(profileResponse.getCorpClass());
        companyProfile.setBusinessNumber(profileResponse.getBusinessNumber());
        companyProfile.setCorporateRegistrationNumber(profileResponse.getCorporateRegistrationNumber());
        companyProfile.setAddress(profileResponse.getAddress());
        companyProfile.setHomepageUrl(profileResponse.getHomepageUrl());
        companyProfile.setIrUrl(profileResponse.getIrUrl());
        companyProfile.setPhoneNumber(profileResponse.getPhoneNumber());
        companyProfile.setFaxNumber(profileResponse.getFaxNumber());
        companyProfile.setIndustryCode(profileResponse.getIndustryCode()); // 업종코드 설정
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
                .stockName(profileResponse.getStockName()) // 종목명 추가
                .ceoName(profileResponse.getCeoName())
                .corpClass(profileResponse.getCorpClass())
                .businessNumber(profileResponse.getBusinessNumber())
                .corporateRegistrationNumber(profileResponse.getCorporateRegistrationNumber())
                .address(profileResponse.getAddress())
                .homepageUrl(profileResponse.getHomepageUrl())
                .irUrl(profileResponse.getIrUrl())
                .phoneNumber(profileResponse.getPhoneNumber())
                .faxNumber(profileResponse.getFaxNumber())
                .industryCode(profileResponse.getIndustryCode()) // 업종코드 설정
                .establishmentDate(profileResponse.getEstablishmentDate())
                .accountingMonth(profileResponse.getAccountingMonth())
                .headquartersId(null) // 소유자 미정 (DART API 업데이트용 임시 프로필)
                .partnerId(null)
                .userType("UNKNOWN") // 소유자 미정 상태
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
                    .corpCode(companyProfile.getCorpCode()) // corp_code 필드 추가
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
            log.debug("공시 정보 저장 완료: receiptNo={}, reportName={}, corpCode={}",
                    item.getReceiptNo(), item.getReportName(), companyProfile.getCorpCode());
        } catch (Exception e) {
            log.error("공시 정보 저장 중 오류 발생: receiptNo={}, corpCode={}",
                    item.getReceiptNo(), companyProfile != null ? companyProfile.getCorpCode() : "N/A", e);
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
     * 기존 데이터는 삭제하지 않고, 새로운 데이터만 추가합니다.
     * 
     * @param corpCode  회사 고유번호
     * @param bsnsYear  사업연도 (YYYY)
     * @param reprtCode 보고서 코드
     */
    private void retrieveAndSaveSingleFinancialStatement(String corpCode, String bsnsYear, String reprtCode) {
        log.info("단일 재무제표 조회 및 저장 시도: corpCode={}, bsnsYear={}, reprtCode={}, fsDiv={}",
                corpCode, bsnsYear, reprtCode, FS_DIV_OFS);
        try {
            // 기존 데이터 조회 (삭제하지 않음)
            List<FinancialStatementData> existingData = financialStatementDataRepository
                    .findByCorpCodeAndBsnsYearAndReprtCode(corpCode, bsnsYear, reprtCode);

            log.info("기존 재무제표 데이터 {}건 확인: corpCode={}, bsnsYear={}, reprtCode={}",
                    existingData.size(), corpCode, bsnsYear, reprtCode);

            // DART API에서 최신 데이터 조회
            FinancialStatementResponseDto responseDto = dartApiService
                    .getFinancialStatement(corpCode, bsnsYear, reprtCode, FS_DIV_OFS).block();

            if (responseDto != null && "000".equals(responseDto.getStatus()) && responseDto.getList() != null
                    && !responseDto.getList().isEmpty()) {
                log.info("재무제표 조회 성공: {}건의 항목. corpCode={}, bsnsYear={}, reprtCode={}",
                        responseDto.getList().size(), corpCode, bsnsYear, reprtCode);

                // 새로운 데이터만 필터링하여 저장
                processAndSaveNewFinancialStatementItems(responseDto.getList(), existingData, corpCode, bsnsYear,
                        reprtCode);
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

    /**
     * 새로운 재무제표 항목만 필터링하여 저장합니다.
     * 기존 데이터와 중복되지 않는 항목만 저장합니다.
     * 
     * @param newItems     API에서 조회한 새로운 항목들
     * @param existingData DB에 이미 존재하는 데이터
     * @param corpCode     회사 고유번호
     * @param bsnsYear     사업연도
     * @param reprtCode    보고서 코드
     */
    private void processAndSaveNewFinancialStatementItems(
            List<FinancialStatementResponseDto.FinancialStatementItem> newItems,
            List<FinancialStatementData> existingData,
            String corpCode, String bsnsYear, String reprtCode) {

        LocalDateTime now = LocalDateTime.now();
        List<FinancialStatementData> dataToSave = new ArrayList<>();

        // 기존 데이터를 빠른 조회를 위해 Set으로 변환 (accountId + sjDiv 조합으로 중복 체크)
        Set<String> existingKeys = existingData.stream()
                .map(data -> generateFinancialDataKey(data.getAccountId(), data.getSjDiv()))
                .collect(Collectors.toSet());

        log.info("기존 재무제표 항목 {}개의 고유 키 생성 완료", existingKeys.size());

        // 새로운 항목들 중 기존에 없는 것만 필터링
        for (FinancialStatementResponseDto.FinancialStatementItem item : newItems) {
            String itemKey = generateFinancialDataKey(item.getAccountId(), item.getSjDiv());

            if (!existingKeys.contains(itemKey)) {
                // 새로운 데이터만 추가
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

                log.debug("새로운 재무제표 항목 추가 대상: accountId={}, accountNm={}",
                        item.getAccountId(), item.getAccountNm());
            } else {
                log.debug("기존 재무제표 항목 스킵: accountId={}, accountNm={}",
                        item.getAccountId(), item.getAccountNm());
            }
        }

        // 새로운 데이터가 있는 경우에만 저장
        if (!dataToSave.isEmpty()) {
            financialStatementDataRepository.saveAll(dataToSave);
            log.info("DB에 새로운 재무제표 항목 {}건 저장 완료: corpCode={}, bsnsYear={}, reprtCode={}",
                    dataToSave.size(), corpCode, bsnsYear, reprtCode);
        } else {
            log.info("저장할 새로운 재무제표 항목 없음 (모든 데이터가 이미 존재): corpCode={}, bsnsYear={}, reprtCode={}",
                    corpCode, bsnsYear, reprtCode);
        }

        log.info("재무제표 데이터 처리 완료 - 전체: {}건, 기존: {}건, 신규 저장: {}건",
                newItems.size(), existingData.size(), dataToSave.size());
    }

    /**
     * 재무제표 항목의 고유 키를 생성합니다.
     * accountId와 sjDiv 조합으로 중복을 판단합니다.
     * 
     * @param accountId 계정 ID
     * @param sjDiv     재무제표 구분
     * @return 고유 키 문자열
     */
    private String generateFinancialDataKey(String accountId, String sjDiv) {
        return String.format("%s|%s",
                accountId != null ? accountId : "NULL",
                sjDiv != null ? sjDiv : "NULL");
    }

    /**
     * 중복된 CompanyProfile들을 하나로 통합합니다.
     * 가장 완성도가 높은 프로필을 기준으로 하고, 나머지는 삭제하지 않고 최적 프로필만 반환합니다.
     * 외래키 제약 조건 때문에 삭제하지 않고 최적 프로필만 사용합니다.
     */
    private CompanyProfile consolidateCompanyProfiles(List<CompanyProfile> profiles, String corpCode) {
        log.info("CompanyProfile 중복 데이터 통합 시작: corpCode={}, 개수={}", corpCode, profiles.size());

        // 가장 완성도가 높은 프로필 선택 (상세 정보가 많은 것 우선)
        CompanyProfile bestProfile = profiles.stream()
                .max((p1, p2) -> {
                    int score1 = calculateProfileCompleteness(p1);
                    int score2 = calculateProfileCompleteness(p2);
                    return Integer.compare(score1, score2);
                })
                .orElse(profiles.get(0));

        log.info("최적 프로필 선택: id={}, corpName={}, 완성도 점수={}",
                bestProfile.getId(), bestProfile.getCorpName(), calculateProfileCompleteness(bestProfile));

        // 중복 프로필들에 대한 정보만 로깅 (삭제하지 않음)
        for (CompanyProfile profile : profiles) {
            if (!profile.getId().equals(bestProfile.getId())) {
                log.info("중복 프로필 발견 (삭제하지 않고 무시): id={}, corpName={}, 완성도 점수={}",
                        profile.getId(), profile.getCorpName(), calculateProfileCompleteness(profile));
            }
        }

        log.info("CompanyProfile 중복 데이터 통합 완료 (최적 프로필만 사용): corpCode={}", corpCode);
        return bestProfile;
    }

    /**
     * CompanyProfile의 완성도를 점수로 계산합니다.
     */
    private int calculateProfileCompleteness(CompanyProfile profile) {
        int score = 0;

        // 기본 정보
        if (profile.getCorpName() != null && !profile.getCorpName().isEmpty())
            score++;
        if (profile.getCeoName() != null && !profile.getCeoName().isEmpty())
            score++;
        if (profile.getAddress() != null && !profile.getAddress().isEmpty())
            score++;
        if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty())
            score++;
        if (profile.getBusinessNumber() != null && !profile.getBusinessNumber().isEmpty())
            score++;
        if (profile.getIndustryCode() != null && !profile.getIndustryCode().isEmpty())
            score++;
        if (profile.getEstablishmentDate() != null && !profile.getEstablishmentDate().isEmpty())
            score++;
        if (profile.getAccountingMonth() != null && !profile.getAccountingMonth().isEmpty())
            score++;

        // 추가 정보
        if (profile.getCorpNameEng() != null && !profile.getCorpNameEng().isEmpty())
            score++;
        if (profile.getStockCode() != null && !profile.getStockCode().isEmpty())
            score++;
        if (profile.getHomepageUrl() != null && !profile.getHomepageUrl().isEmpty())
            score++;
        if (profile.getFaxNumber() != null && !profile.getFaxNumber().isEmpty())
            score++;

        return score;
    }
}
