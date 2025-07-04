/**
 * @file PartnerCompanyApiService.java
 * @description 협력 회사 API와 통신하기 위한 서비스입니다.
 *              협력 회사 정보의 CRUD 기능을 제공하며, CompanyProfile과의 연관관계를 통해 회사 정보를 관리합니다.
 */
package com.nsmm.esg.dart_service.partner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.nsmm.esg.dart_service.database.entity.PartnerCompany;
import com.nsmm.esg.dart_service.database.entity.CompanyProfile;
import com.nsmm.esg.dart_service.database.entity.DartCorpCode;
import com.nsmm.esg.dart_service.database.repository.PartnerCompanyRepository;
import com.nsmm.esg.dart_service.database.repository.CompanyProfileRepository;
import com.nsmm.esg.dart_service.database.repository.DartCorpCodeRepository;
import com.nsmm.esg.dart_service.kafka.service.KafkaProducerService;
import com.nsmm.esg.dart_service.partner.dto.CreatePartnerCompanyDto;
import com.nsmm.esg.dart_service.partner.dto.PaginatedPartnerCompanyResponseDto;
import com.nsmm.esg.dart_service.partner.dto.PartnerCompanyKafkaMessage;
import com.nsmm.esg.dart_service.partner.dto.PartnerCompanyResponseDto;
import com.nsmm.esg.dart_service.partner.dto.UpdatePartnerCompanyDto;
import com.nsmm.esg.dart_service.partner.model.PartnerCompanyStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerCompanyApiService {

    @Value("${partner.api.base-url}")
    private String baseUrl;

    @Value("${partner.api.client-id}")
    private String clientId;

    @Value("${partner.api.client-secret}")
    private String clientSecret;

    @Value("${kafka.topic.partner-company}")
    private String partnerCompanyTopic;

    private final WebClient.Builder webClientBuilder;
    private final PartnerCompanyRepository partnerCompanyRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final DartCorpCodeRepository dartCorpCodeRepository;
    private final KafkaProducerService kafkaProducerService;

    /**
     * 협력 API에서 회사 정보를 조회합니다.
     * 
     * @param companyId 회사 ID
     * @return 회사 정보
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCompanyInfo(String companyId) {
        log.info("협력 API 호출 - 회사 정보 조회: {}", companyId);

        return webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Client-Id", clientId)
                .defaultHeader("X-Client-Secret", clientSecret)
                .build()
                .get()
                .uri("/companies/{companyId}", companyId)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(e -> log.error("협력 API 호출 실패: {}", e.getMessage()))
                .block();
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 협력 API에서 회사의 재무 정보를 조회합니다.
     * 
     * @param companyId 회사 ID
     * @param year      조회 연도
     * @param quarter   조회 분기
     * @return 재무 정보
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFinancialInfo(String companyId, int year, int quarter) {
        log.info("협력 API 호출 - 재무 정보 조회: {}, {}년 {}분기", companyId, year, quarter);

        return webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Client-Id", clientId)
                .defaultHeader("X-Client-Secret", clientSecret)
                .build()
                .get()
                .uri("/companies/{companyId}/financials?year={year}&quarter={quarter}",
                        companyId, year, quarter)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(e -> log.error("협력 API 호출 실패: {}", e.getMessage()))
                .block();
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 새로운 협력 회사를 생성합니다.
     * 
     * @param createDto       생성할 협력 회사 정보
     * @param headquartersId  본사 ID (게이트웨이 헤더 X-HEADQUARTERS-ID)
     * @param partnerId       협력사 사용자 ID (게이트웨이 헤더 X-PARTNER-ID, 선택사항)
     * @param hqAccountNumber 본사 계정 번호
     * @return 생성된 협력 회사 정보
     */
    @Transactional
    public PartnerCompanyResponseDto createPartnerCompany(CreatePartnerCompanyDto createDto,
            Long headquartersId, Long partnerId, String hqAccountNumber) {
        log.info("협력 회사 생성 요청 - corpCode: {}, 본사 ID: {}, 협력사 ID: {}",
                createDto.getCorpCode(), headquartersId, partnerId);

        // 1. CompanyProfile 존재 여부 확인 또는 생성
        CompanyProfile companyProfile = getOrCreateCompanyProfile(createDto.getCorpCode(), headquartersId, partnerId);
        if (companyProfile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "해당 기업코드의 회사 정보를 찾을 수 없습니다. corpCode: " + createDto.getCorpCode());
        }

        // 2. 본사별/협력사별 ACTIVE 상태의 중복 회사명 검사
        // 실제 요청자 타입에 따른 중복 검사
        Optional<PartnerCompany> activePartner;
        if (partnerId != null) {
            // 협력사인 경우: 해당 협력사가 등록한 동일 회사명 검사
            activePartner = partnerCompanyRepository
                    .findByPartnerIdAndCompanyNameIgnoreCaseAndStatus(partnerId, companyProfile.getCorpName(),
                            PartnerCompanyStatus.ACTIVE);
        } else {
            // 본사인 경우: 해당 본사가 등록한 동일 회사명 검사
            activePartner = partnerCompanyRepository
                    .findByHeadquartersIdAndCompanyNameIgnoreCaseAndStatus(headquartersId, companyProfile.getCorpName(),
                            PartnerCompanyStatus.ACTIVE);
        }

        if (activePartner.isPresent()) {
            log.info("동일한 본사/협력사에서 이미 등록된 협력사 정보 반환 - 회사명: {}, 본사ID: {}, 협력사ID: {}",
                    companyProfile.getCorpName(), headquartersId, partnerId);
            return mapToResponseDto(activePartner.get(), false);
        }

        // 3. INACTIVE 상태의 동일한 회사명이 있는지 확인하고 복원 (본사별/협력사별)
        Optional<PartnerCompany> inactivePartner;
        if (partnerId != null) {
            // 협력사인 경우: 해당 협력사가 등록한 INACTIVE 회사명 검사
            inactivePartner = partnerCompanyRepository
                    .findByPartnerIdAndCompanyNameIgnoreCaseAndStatus(partnerId, companyProfile.getCorpName(),
                            PartnerCompanyStatus.INACTIVE);
        } else {
            // 본사인 경우: 해당 본사가 등록한 INACTIVE 회사명 검사
            inactivePartner = partnerCompanyRepository
                    .findByHeadquartersIdAndCompanyNameIgnoreCaseAndStatus(headquartersId, companyProfile.getCorpName(),
                            PartnerCompanyStatus.INACTIVE);
        }

        if (inactivePartner.isPresent()) {
            log.info("INACTIVE 상태의 기존 협력사 발견 - 복원 처리: {}", companyProfile.getCorpName());
            PartnerCompany existingPartner = inactivePartner.get();

            // 실제 요청자 타입 결정
            String actualUserType = (partnerId != null) ? "PARTNER" : "HEADQUARTERS";
            Long actualHeadquartersId = actualUserType.equals("HEADQUARTERS") ? headquartersId : null;
            Long actualPartnerId = actualUserType.equals("PARTNER") ? partnerId : null;

            // 기존 데이터를 새로운 정보로 업데이트하고 ACTIVE로 복원
            existingPartner.setCompanyProfile(companyProfile);
            existingPartner.setCorpCode(createDto.getCorpCode());
            existingPartner.setContractStartDate(createDto.getContractStartDate());
            existingPartner.setStatus(PartnerCompanyStatus.ACTIVE);
            existingPartner.setHeadquartersId(actualHeadquartersId);
            existingPartner.setPartnerId(actualPartnerId);
            existingPartner.setUserType(actualUserType);
            existingPartner.setUpdatedAt(LocalDateTime.now());

            PartnerCompany restoredPartner = partnerCompanyRepository.save(existingPartner);
            log.info("협력사 복원 완료 - ID: {}", restoredPartner.getId());

            // Kafka 메시지 발행 (복원 이벤트)
            publishPartnerCompanyKafkaMessage(createDto.getCorpCode());

            return mapToResponseDto(restoredPartner, true);
        }

        // 4. 새로운 협력 회사 생성
        String newId = UUID.randomUUID().toString();

        // 실제 요청자 타입 결정
        String actualUserType = (partnerId != null) ? "PARTNER" : "HEADQUARTERS";
        Long actualHeadquartersId = actualUserType.equals("HEADQUARTERS") ? headquartersId : null;
        Long actualPartnerId = actualUserType.equals("PARTNER") ? partnerId : null;

        PartnerCompany newPartner = PartnerCompany.builder()
                .id(newId)
                .corpCode(createDto.getCorpCode())
                .companyProfile(companyProfile)
                .headquartersId(actualHeadquartersId)
                .partnerId(actualPartnerId)
                .userType(actualUserType)
                .contractStartDate(createDto.getContractStartDate())
                .status(PartnerCompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PartnerCompany savedPartner = partnerCompanyRepository.save(newPartner);
        log.info("새로운 협력사 생성 완료 - ID: {}, 회사명: {}", savedPartner.getId(), companyProfile.getCorpName());

        // Kafka 메시지 발행
        publishPartnerCompanyKafkaMessage(createDto.getCorpCode());

        return mapToResponseDto(savedPartner, false);
    }
    //------------------------------------------------------------------------------------------------------------------


    /**
     * CompanyProfile을 조회하거나 DartCorpCode에서 생성합니다.
     * 
     * @param corpCode       기업 코드
     * @param headquartersId 본사 ID
     * @param partnerId      협력사 ID
     * @return CompanyProfile 또는 null
     */
    private CompanyProfile getOrCreateCompanyProfile(String corpCode, Long headquartersId, Long partnerId) {
        // 1. 실제 요청자 타입 결정 (partnerId가 null이 아니면 협력사 요청)
        String actualUserType = (partnerId != null) ? "PARTNER" : "HEADQUARTERS";
        Long actualOwnerId = (partnerId != null) ? partnerId : headquartersId;

        log.info("CompanyProfile 조회/생성 - corpCode: {}, actualUserType: {}, actualOwnerId: {}",
                corpCode, actualUserType, actualOwnerId);

        // 2. 해당 소유자의 CompanyProfile 조회
        Optional<CompanyProfile> existingProfile;
        if ("PARTNER".equals(actualUserType)) {
            existingProfile = companyProfileRepository.findByPartnerIdAndCorpCode(partnerId, corpCode);
        } else {
            existingProfile = companyProfileRepository.findByHeadquartersIdAndCorpCode(headquartersId, corpCode);
        }

        if (existingProfile.isPresent()) {
            log.info("기존 CompanyProfile 발견: corpCode={}, userType={}, ownerId={}, corpName={}",
                    corpCode, actualUserType, actualOwnerId, existingProfile.get().getCorpName());
            return existingProfile.get();
        }

        // 3. CompanyProfile이 없으면 DartCorpCode에서 조회하여 생성
        log.info("CompanyProfile이 없음. DartCorpCode에서 조회하여 생성: corpCode={}", corpCode);

        Optional<DartCorpCode> dartCorpCodeOpt = dartCorpCodeRepository.findById(corpCode);
        if (dartCorpCodeOpt.isEmpty()) {
            log.error("DartCorpCode에서도 찾을 수 없음: corpCode={}", corpCode);
            return null;
        }

        DartCorpCode dartCorpCode = dartCorpCodeOpt.get();
        log.info("DartCorpCode에서 회사 정보 발견: corpCode={}, corpName={}, stockCode={}",
                corpCode, dartCorpCode.getCorpName(), dartCorpCode.getStockCode());

        // 4. DartCorpCode 정보를 기반으로 CompanyProfile 생성
        LocalDateTime now = LocalDateTime.now();
        CompanyProfile newProfile = CompanyProfile.builder()
                .corpCode(corpCode)
                .headquartersId("HEADQUARTERS".equals(actualUserType) ? headquartersId : null)
                .partnerId("PARTNER".equals(actualUserType) ? partnerId : null)
                .userType(actualUserType)
                .corpName(dartCorpCode.getCorpName())
                .corpNameEng(dartCorpCode.getCorpEngName())
                .stockCode(dartCorpCode.getStockCode())
                .stockName(!StringUtils.hasText(dartCorpCode.getStockCode()) ? null : dartCorpCode.getCorpName())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 5. CompanyProfile 저장
        CompanyProfile savedProfile = companyProfileRepository.save(newProfile);
        log.info("DartCorpCode 기반 CompanyProfile 생성 완료: corpCode={}, userType={}, ownerId={}, corpName={}",
                corpCode, actualUserType, actualOwnerId, savedProfile.getCorpName());

        // 6. 백그라운드에서 DART API 호출하여 상세 정보 업데이트 (비동기)
        publishPartnerCompanyKafkaMessage(corpCode);

        return savedProfile;
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Kafka 메시지를 발행합니다.
     */
    private void publishPartnerCompanyKafkaMessage(String corpCode) {
        try {
            PartnerCompanyKafkaMessage message = PartnerCompanyKafkaMessage.builder()
                    .corpCode(corpCode)
                    .action("partner_company_registered")
                    .timestamp(LocalDateTime.now().toString())
                    .build();

            CompletableFuture<SendResult<String, Object>> future = kafkaProducerService
                    .sendMessage(partnerCompanyTopic, corpCode, message);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka 메시지 발행 실패 - corpCode: {}, 오류: {}", corpCode, ex.getMessage());
                } else {
                    log.info("Kafka 메시지 발행 성공 - corpCode: {}", corpCode);
                }
            });
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 중 예외 발생 - corpCode: {}, 오류: {}", corpCode, e.getMessage());
        }
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 모든 협력 회사 목록을 조회합니다.
     * 
     * @param page        페이지 번호 (1부터 시작)
     * @param pageSize    페이지당 항목 수
     * @param companyName 회사명 필터 (부분 일치)
     * @return 페이지네이션을 포함한 협력 회사 목록
     */
    @Transactional(readOnly = true)
    public PaginatedPartnerCompanyResponseDto findAllPartnerCompanies(int page, int pageSize, String companyName) {
        log.info("모든 협력 회사 목록 조회 요청 - 페이지: {}, 페이지 크기: {}, 회사명 필터: {}", page, pageSize, companyName);

        int validPage = Math.max(1, page);
        int validPageSize = Math.max(1, Math.min(100, pageSize));

        Pageable pageable = PageRequest.of(validPage - 1, validPageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PartnerCompany> partnerCompaniesPage;
        if (companyName != null && !companyName.isEmpty()) {
            partnerCompaniesPage = partnerCompanyRepository.findByCompanyNameContainingIgnoreCaseAndStatus(
                    companyName, PartnerCompanyStatus.ACTIVE, pageable);
        } else {
            partnerCompaniesPage = partnerCompanyRepository.findByStatus(PartnerCompanyStatus.ACTIVE, pageable);
        }

        List<PartnerCompanyResponseDto> partnerCompanies = partnerCompaniesPage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        return PaginatedPartnerCompanyResponseDto.builder()
                .data(partnerCompanies)
                .total(partnerCompaniesPage.getTotalElements())
                .page(validPage)
                .pageSize(validPageSize)
                .build();
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 특정 협력 회사를 조회합니다.
     * 
     * @param id 협력 회사 ID
     * @return 협력 회사 정보
     */
    @Transactional(readOnly = true)
    public PartnerCompanyResponseDto findPartnerCompanyById(String id) {
        log.info("협력 회사 ID로 조회 요청 - ID: {}", id);

        PartnerCompany partnerCompany = partnerCompanyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "ID '" + id + "'에 해당하는 협력사를 찾을 수 없습니다."));

        return mapToResponseDto(partnerCompany);
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 협력 회사 정보를 업데이트합니다.
     * 
     * @param id        협력 회사 ID
     * @param updateDto 업데이트할 정보
     * @return 업데이트된 협력 회사 정보
     */
    @Transactional
    public PartnerCompanyResponseDto updatePartnerCompany(String id, UpdatePartnerCompanyDto updateDto) {
        log.info("협력 회사 정보 업데이트 요청 - ID: {}", id);

        PartnerCompany partnerCompany = partnerCompanyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "ID '" + id + "'에 해당하는 협력사를 찾을 수 없습니다."));

        // corpCode 변경 시 CompanyProfile 업데이트
        if (updateDto.getCorpCode() != null && !updateDto.getCorpCode().isEmpty()) {
            Optional<CompanyProfile> newCompanyProfile = companyProfileRepository
                    .findByCorpCode(updateDto.getCorpCode());
            if (newCompanyProfile.isPresent()) {
                partnerCompany.setCompanyProfile(newCompanyProfile.get());
                log.info("협력사 CompanyProfile 업데이트: ID={}, 새로운 corpCode={}", id, updateDto.getCorpCode());
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "새로운 기업코드 '" + updateDto.getCorpCode() + "'에 해당하는 회사 정보를 찾을 수 없습니다.");
            }
        }

        // 업데이트 가능한 필드들만 수정
        if (updateDto.getContractStartDate() != null) {
            partnerCompany.setContractStartDate(updateDto.getContractStartDate());
        }

        if (updateDto.getStatus() != null) {
            partnerCompany.setStatus(updateDto.getStatus());
        }

        partnerCompany.setUpdatedAt(LocalDateTime.now());
        PartnerCompany updatedPartner = partnerCompanyRepository.save(partnerCompany);

        log.info("협력 회사 정보 업데이트 완료 - ID: {}", updatedPartner.getId());
        return mapToResponseDto(updatedPartner);
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 협력 회사를 삭제합니다. (INACTIVE 상태로 변경)
     * 
     * @param id 협력 회사 ID
     * @return 삭제 결과
     */
    @Transactional
    public Map<String, String> deletePartnerCompany(String id) {
        log.info("협력 회사 삭제 요청 - ID: {}", id);

        PartnerCompany partnerCompany = partnerCompanyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "ID '" + id + "'에 해당하는 협력사를 찾을 수 없습니다."));

        partnerCompany.setStatus(PartnerCompanyStatus.INACTIVE);
        partnerCompany.setUpdatedAt(LocalDateTime.now());
        partnerCompanyRepository.save(partnerCompany);

        log.info("협력 회사 비활성화 완료 - ID: {}", id);
        return Map.of("message", "협력사가 성공적으로 비활성화되었습니다.", "id", id);
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 협력 회사 엔티티를 응답 DTO로 변환합니다.
     * 
     * @param partnerCompany 협력 회사 엔티티
     * @return 협력 회사 응답 DTO
     */
    private PartnerCompanyResponseDto mapToResponseDto(PartnerCompany partnerCompany) {
        return mapToResponseDto(partnerCompany, false);
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 협력 회사 엔티티를 응답 DTO로 변환합니다. (복원 플래그 포함)
     * 
     * @param partnerCompany 협력 회사 엔티티
     * @param isRestored     복원 여부
     * @return 협력 회사 응답 DTO
     */
    private PartnerCompanyResponseDto mapToResponseDto(PartnerCompany partnerCompany, boolean isRestored) {
        if (partnerCompany == null) {
            return null;
        }

        CompanyProfile profile = partnerCompany.getCompanyProfile();

        return PartnerCompanyResponseDto.builder()
                .id(partnerCompany.getId())
                .corpCode(partnerCompany.getCorpCode())
                .status(partnerCompany.getStatus())
                .contractStartDate(partnerCompany.getContractStartDate())
                .createdAt(partnerCompany.getCreatedAt())
                .updatedAt(partnerCompany.getUpdatedAt())
                .accountCreated(partnerCompany.getAccountCreated())
                // 소유자 정보
                .headquartersId(partnerCompany.getHeadquartersId())
                .partnerId(partnerCompany.getPartnerId())
                .userType(partnerCompany.getUserType())
                // CompanyProfile 정보
                .corpName(profile != null ? profile.getCorpName() : "정보 없음")
                .corpNameEng(profile != null ? profile.getCorpNameEng() : null)
                .stockCode(profile != null ? profile.getStockCode() : null)
                .stockName(profile != null ? profile.getStockName() : null)
                .ceoName(profile != null ? profile.getCeoName() : null)
                .corpClass(profile != null ? profile.getCorpClass() : null)
                .businessNumber(profile != null ? profile.getBusinessNumber() : null)
                .corporateRegistrationNumber(profile != null ? profile.getCorporateRegistrationNumber() : null)
                .address(profile != null ? profile.getAddress() : null)
                .homepageUrl(profile != null ? profile.getHomepageUrl() : null)
                .irUrl(profile != null ? profile.getIrUrl() : null)
                .phoneNumber(profile != null ? profile.getPhoneNumber() : null)
                .faxNumber(profile != null ? profile.getFaxNumber() : null)
                .industryCode(profile != null ? profile.getIndustryCode() : null)
                .establishmentDate(profile != null ? profile.getEstablishmentDate() : null)
                .accountingMonth(profile != null ? profile.getAccountingMonth() : null)
                .companyProfileUpdatedAt(profile != null ? profile.getUpdatedAt() : null)
                .build();
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 특정 본사/협력사의 협력 회사 목록을 조회합니다.
     * 
     * @param headquartersId 본사 ID
     * @param partnerId      협력사 사용자 ID (선택사항)
     * @param page           페이지 번호 (1부터 시작)
     * @param pageSize       페이지당 항목 수
     * @param companyName    회사명 필터 (부분 일치)
     * @return 페이지네이션을 포함한 협력 회사 목록
     */
    @Transactional(readOnly = true)
    public PaginatedPartnerCompanyResponseDto findAllPartnerCompaniesByOrganization(
            Long headquartersId, Long partnerId, int page, int pageSize, String companyName) {
        log.info("조직별 협력 회사 목록 조회 - 본사 ID: {}, 협력사 ID: {}, 페이지: {}, 크기: {}, 필터: {}",
                headquartersId, partnerId, page, pageSize, companyName);

        int validPage = Math.max(1, page);
        int validPageSize = Math.max(1, Math.min(100, pageSize));

        Pageable pageable = PageRequest.of(validPage - 1, validPageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PartnerCompany> partnerCompaniesPage;

        if (partnerId != null) {
            // 협력사 사용자인 경우 자신이 등록한 협력사만 조회
            if (companyName != null && !companyName.isEmpty()) {
                partnerCompaniesPage = partnerCompanyRepository
                        .findByPartnerIdAndCompanyNameContainingIgnoreCaseAndStatus(
                                partnerId, companyName, PartnerCompanyStatus.ACTIVE, pageable);
            } else {
                partnerCompaniesPage = partnerCompanyRepository.findByPartnerIdAndStatus(
                        partnerId, PartnerCompanyStatus.ACTIVE, pageable);
            }
        } else {
            // 본사 사용자인 경우 해당 본사의 모든 협력사 조회
            if (companyName != null && !companyName.isEmpty()) {
                partnerCompaniesPage = partnerCompanyRepository
                        .findByHeadquartersIdAndCompanyNameContainingIgnoreCaseAndStatus(
                                headquartersId, companyName, PartnerCompanyStatus.ACTIVE, pageable);
            } else {
                partnerCompaniesPage = partnerCompanyRepository.findByHeadquartersIdAndStatus(
                        headquartersId, PartnerCompanyStatus.ACTIVE, pageable);
            }
        }

        List<PartnerCompanyResponseDto> partnerCompanies = partnerCompaniesPage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        return PaginatedPartnerCompanyResponseDto.builder()
                .data(partnerCompanies)
                .total(partnerCompaniesPage.getTotalElements())
                .page(validPage)
                .pageSize(validPageSize)
                .build();
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 시스템에 등록된 모든 활성 상태의 협력사들의 고유한 회사명 목록을 조회합니다.
     * 
     * @return 고유한 협력사명 목록
     */
    @Transactional(readOnly = true)
    public List<String> getUniqueActivePartnerCompanyNames() {
        log.info("모든 활성 협력사의 고유한 회사명 목록 조회 요청");
        List<PartnerCompany> activeCompanies = partnerCompanyRepository.findByStatus(PartnerCompanyStatus.ACTIVE);
        return activeCompanies.stream()
                .filter(company -> company.getCompanyProfile() != null)
                .map(company -> company.getCompanyProfile().getCorpName())
                .distinct()
                .collect(Collectors.toList());
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 협력사 회사명 중복 검사를 수행합니다.
     * 
     * @param companyName 검사할 회사명
     * @param excludeId   제외할 협력사 ID (수정 시 자기 자신 제외용)
     * @return 중복 검사 결과
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkCompanyNameDuplicate(String companyName, String excludeId) {
        log.info("협력사 회사명 중복 검사 - 회사명: {}, 제외 ID: {}", companyName, excludeId);

        if (companyName == null || companyName.trim().isEmpty()) {
            return Map.of(
                    "isDuplicate", false,
                    "message", "회사명이 제공되지 않았습니다.");
        }

        // 동일한 회사명을 가진 활성 상태의 협력사 검색
        Optional<PartnerCompany> existingCompany = partnerCompanyRepository
                .findByCompanyNameIgnoreCaseAndStatus(companyName.trim(), PartnerCompanyStatus.ACTIVE);

        if (existingCompany.isPresent()) {
            PartnerCompany existing = existingCompany.get();

            // 제외할 ID가 있고, 그것이 발견된 회사와 같다면 중복이 아님
            if (excludeId != null && excludeId.equals(existing.getId())) {
                return Map.of(
                        "isDuplicate", false,
                        "message", "수정 중인 자기 자신의 회사명입니다.");
            }

            // 실제 중복
            return Map.of(
                    "isDuplicate", true,
                    "message", String.format("'%s' 이름의 협력사가 이미 등록되어 있습니다.", companyName),
                    "existingCompanyId", existing.getId());
        }

        // 중복 없음
        return Map.of(
                "isDuplicate", false,
                "message", "사용 가능한 회사명입니다.");
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * ID로 협력 회사 엔티티를 조회합니다.
     */
    public PartnerCompany findEntityById(String id) {
        return partnerCompanyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "ID '" + id + "'에 해당하는 협력사를 찾을 수 없습니다."));
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 협력 회사 엔티티를 저장합니다.
     */
    public PartnerCompany save(PartnerCompany partnerCompany) {
        return partnerCompanyRepository.save(partnerCompany);
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 협력 회사의 계정 생성 상태를 업데이트합니다.
     */
    @Transactional
    public void updateAccountCreatedStatus(String id, boolean accountCreated) {
        PartnerCompany partnerCompany = findEntityById(id);
        partnerCompany.setAccountCreated(accountCreated);
        partnerCompany.setUpdatedAt(LocalDateTime.now());
        save(partnerCompany);
        log.info("협력사 계정 생성 상태 업데이트 완료 - ID: {}, accountCreated: {}", id, accountCreated);
    }
    //------------------------------------------------------------------------------------------------------------------
}
