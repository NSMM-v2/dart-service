/**
 * @file PartnerCompanyApiService.java
 * @description 파트너 회사 API와 통신하기 위한 서비스입니다.
 *              파트너 회사 정보의 CRUD 기능을 제공합니다.
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.nsmm.esg.dart_service.dart.dto.CompanyProfileResponse;
import com.nsmm.esg.dart_service.dart.service.DartApiService;
import com.nsmm.esg.dart_service.database.entity.PartnerCompany;
import com.nsmm.esg.dart_service.database.repository.PartnerCompanyRepository;
import com.nsmm.esg.dart_service.kafka.service.KafkaProducerService;
import com.nsmm.esg.dart_service.partner.dto.CreatePartnerCompanyDto;
import com.nsmm.esg.dart_service.partner.dto.PaginatedPartnerCompanyResponseDto;
import com.nsmm.esg.dart_service.partner.dto.PartnerCompanyResponseDto;
import com.nsmm.esg.dart_service.partner.dto.UpdatePartnerCompanyDto;
import com.nsmm.esg.dart_service.partner.model.PartnerCompanyStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final DartApiService dartApiService;
    private final KafkaProducerService kafkaProducerService;

    /**
     * 파트너 API에서 회사 정보를 조회합니다.
     * 
     * @param companyId 회사 ID
     * @return 회사 정보
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCompanyInfo(String companyId) {
        log.info("파트너 API 호출 - 회사 정보 조회: {}", companyId);

        return webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Client-Id", clientId)
                .defaultHeader("X-Client-Secret", clientSecret)
                .build()
                .get()
                .uri("/companies/{companyId}", companyId)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(e -> log.error("파트너 API 호출 실패: {}", e.getMessage()))
                .block();
    }

    /**
     * 파트너 API에서 회사의 재무 정보를 조회합니다.
     * 
     * @param companyId 회사 ID
     * @param year 조회 연도
     * @param quarter 조회 분기
     * @return 재무 정보
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFinancialInfo(String companyId, int year, int quarter) {
        log.info("파트너 API 호출 - 재무 정보 조회: {}, {}년 {}분기", companyId, year, quarter);

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
                .doOnError(e -> log.error("파트너 API 호출 실패: {}", e.getMessage()))
                .block();
    }

    /**
     * 새로운 파트너 회사를 생성합니다.
     * 
     * @param createDto 생성할 파트너 회사 정보
     * @param memberId 요청한 회원 ID
     * @return 생성된 파트너 회사 정보
     */
    @Transactional
    public PartnerCompanyResponseDto createPartnerCompany(CreatePartnerCompanyDto createDto, String memberId) {
        log.info("파트너 회사 생성 요청 - 회사명: {}, 회원 ID: {}", createDto.getCompanyName(), memberId);

        // 1. ACTIVE 상태의 중복 회사명 검사
        Optional<PartnerCompany> activePartner = partnerCompanyRepository
                .findByCompanyNameIgnoreCaseAndStatus(createDto.getCompanyName(), PartnerCompanyStatus.ACTIVE);
        
        if (activePartner.isPresent()) {
            log.warn("ACTIVE 상태의 중복된 회사명으로 파트너사 등록 시도 - 회사명: {}", createDto.getCompanyName());
            
            // 현재 사용자의 파트너사인지 확인
            if (activePartner.get().getMemberId().equals(memberId)) {
                log.info("이미 등록된 파트너사 정보 반환 - 회사명: {}, 회원 ID: {}", createDto.getCompanyName(), memberId);
                
                // 기존 파트너사 정보를 업데이트하고 정상 응답으로 반환
                PartnerCompany existingPartner = activePartner.get();
                
                // 필요시 정보 업데이트
                boolean needsUpdate = false;
                if (!existingPartner.getCorpCode().equals(createDto.getCorpCode())) {
                    existingPartner.setCorpCode(createDto.getCorpCode());
                    needsUpdate = true;
                }
                if (!existingPartner.getContractStartDate().equals(createDto.getContractStartDate())) {
                    existingPartner.setContractStartDate(createDto.getContractStartDate());
                    needsUpdate = true;
                }
                if (createDto.getStockCode() != null && !createDto.getStockCode().equals(existingPartner.getStockCode())) {
                    existingPartner.setStockCode(createDto.getStockCode());
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    existingPartner.setUpdatedAt(LocalDateTime.now());
                    existingPartner = partnerCompanyRepository.save(existingPartner);
                    log.info("기존 파트너사 정보 업데이트 완료 - ID: {}", existingPartner.getId());
                }
                
                // 기존 파트너사 데이터를 정상 응답으로 반환 (이미 존재하므로 is_restored: false)
                return mapToResponseDto(existingPartner, false);
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    String.format("'%s' 이름의 파트너사가 이미 시스템에 등록되어 있습니다.", createDto.getCompanyName()));
            }
        }

        // 2. INACTIVE 상태의 동일한 회사명이 있는지 확인하고 복원
        Optional<PartnerCompany> inactivePartner = partnerCompanyRepository
                .findByCompanyNameIgnoreCaseAndStatus(createDto.getCompanyName(), PartnerCompanyStatus.INACTIVE);
        
        if (inactivePartner.isPresent()) {
            log.info("INACTIVE 상태의 기존 파트너사 발견 - 복원 처리: {}", createDto.getCompanyName());
            PartnerCompany existingPartner = inactivePartner.get();
            
            // 기존 데이터를 새로운 정보로 업데이트하고 ACTIVE로 복원
            existingPartner.setCorpCode(createDto.getCorpCode());
            existingPartner.setContractStartDate(createDto.getContractStartDate());
            existingPartner.setStatus(PartnerCompanyStatus.ACTIVE);
            existingPartner.setUpdatedAt(LocalDateTime.now());
            existingPartner.setMemberId(memberId); // 현재 사용자로 변경
            
            // stockCode 업데이트
            String stockCode = createDto.getStockCode();
            if (stockCode == null || stockCode.trim().isEmpty()) {
                try {
                    CompanyProfileResponse dartProfile = dartApiService.getCompanyProfile(createDto.getCorpCode()).block();
                    if (dartProfile != null) {
                        stockCode = dartProfile.getStockCode();
                    }
                } catch (Exception e) {
                    log.warn("DART API 호출 중 오류 발생: {}", e.getMessage());
                }
            }
            existingPartner.setStockCode(stockCode);
            
            PartnerCompany restoredPartner = partnerCompanyRepository.save(existingPartner);
            log.info("파트너사 복원 완료 - ID: {}", restoredPartner.getId());
            
            // Kafka 메시지 발행 (복원 이벤트)
            try {
                CompletableFuture<SendResult<String, Object>> future =
                        kafkaProducerService.sendPartnerRestoreEvent(restoredPartner.getId().toString(), mapToResponseDto(restoredPartner));
                future.whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("파트너사 복원 이벤트 발행 성공: {}", restoredPartner.getId());
                    } else {
                        log.error("파트너사 복원 이벤트 발행 실패: {}", ex.getMessage());
                    }
                });
            } catch (Exception e) {
                log.error("파트너사 복원 이벤트 발행 중 오류: {}", e.getMessage());
            }
            
            return mapToResponseDto(restoredPartner, true); // 복원됨을 표시
        }

        // 3. 새로운 파트너사 생성 (기존 로직)

        // 프론트엔드에서 제공된 stockCode 우선 사용
        String stockCode = createDto.getStockCode();
        String finalCorpCode = createDto.getCorpCode();

        // stockCode가 제공되지 않았거나 비어있는 경우 DART API에서 조회
        if (stockCode == null || stockCode.trim().isEmpty()) {
            try {
                CompanyProfileResponse dartProfile = dartApiService.getCompanyProfile(createDto.getCorpCode()).block();
                if (dartProfile != null) {
                    log.info("DART API에서 회사 정보 조회 성공 - 회사명: {}", dartProfile.getCorpName());
                    stockCode = dartProfile.getStockCode();

                    // DART API에서 반환한 corpCode가 다른 경우 업데이트
                    if (dartProfile.getCorpCode() != null && !dartProfile.getCorpCode().equals(finalCorpCode)) {
                        log.info("DART API에서 반환한 corpCode({})가 요청한 corpCode({})와 다릅니다. DART API 값을 사용합니다.", 
                                dartProfile.getCorpCode(), finalCorpCode);
                        finalCorpCode = dartProfile.getCorpCode();
                    }
                }
            } catch (Exception e) {
                log.warn("DART API 호출 중 오류 발생: {}", e.getMessage());
            }
        } else {
            log.info("프론트엔드에서 제공된 주식코드 사용: {}", stockCode);
        }

        // 파트너 회사 엔티티 생성
        PartnerCompany partnerCompany = PartnerCompany.builder()
                .memberId(memberId)
                .companyName(createDto.getCompanyName())
                .corpCode(finalCorpCode)
                .stockCode(stockCode)
                .contractStartDate(createDto.getContractStartDate())
                .status(PartnerCompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 데이터베이스에 저장
        PartnerCompany savedPartnerCompany = partnerCompanyRepository.save(partnerCompany);
        log.info("파트너 회사 생성 완료 - ID: {}", savedPartnerCompany.getId());

        // 응답 DTO 생성
        PartnerCompanyResponseDto responseDto = mapToResponseDto(savedPartnerCompany);

        // Kafka 메시지 발행
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaProducerService.sendMessage(partnerCompanyTopic, savedPartnerCompany.getId().toString(), responseDto);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("파트너 회사 생성 메시지 발행 성공 - ID: {}", savedPartnerCompany.getId());
                } else {
                    log.error("파트너 회사 생성 메시지 발행 실패 - ID: {}", savedPartnerCompany.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 중 오류 발생", e);
        }

        return responseDto;
    }

    /**
     * 파트너 회사 목록을 조회합니다.
     * 
     * @param page 페이지 번호 (1부터 시작)
     * @param pageSize 페이지당 항목 수
     * @param companyName 회사명 필터 (부분 일치)
     * @return 페이지네이션을 포함한 파트너 회사 목록
     */
    @Transactional(readOnly = true)
    public PaginatedPartnerCompanyResponseDto findAllPartnerCompanies(int page, int pageSize, String companyName) {
        log.info("파트너 회사 목록 조회 요청 - 페이지: {}, 페이지 크기: {}, 회사명 필터: {}", page, pageSize, companyName);

        // 페이지 번호 검증 및 보정
        int validPage = Math.max(1, page);
        int validPageSize = Math.max(1, Math.min(100, pageSize));
        
        log.debug("검증된 페이지 파라미터 - 원본: page={}, pageSize={} -> 보정: page={}, pageSize={}", 
                page, pageSize, validPage, validPageSize);

        // 페이지 번호는 0부터 시작하므로 1을 빼줌 (이제 항상 0 이상이 보장됨)
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

    /**
     * ID로 파트너 회사를 조회합니다.
     * 
     * @param id 파트너 회사 ID
     * @return 파트너 회사 정보
     * @throws ResponseStatusException 파트너 회사를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PartnerCompanyResponseDto findPartnerCompanyById(String id) {
        log.info("파트너 회사 조회 요청 - ID: {}", id);

        PartnerCompany partnerCompany = partnerCompanyRepository.findByIdAndStatus(id, PartnerCompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        String.format("ID '%s'에 해당하는 파트너사를 찾을 수 없습니다.", id)));

        return mapToResponseDto(partnerCompany);
    }

    /**
     * 파트너 회사 정보를 업데이트합니다.
     * 
     * @param id 파트너 회사 ID
     * @param updateDto 업데이트할 파트너 회사 정보
     * @return 업데이트된 파트너 회사 정보
     * @throws ResponseStatusException 파트너 회사를 찾을 수 없는 경우
     */
    @Transactional
    public PartnerCompanyResponseDto updatePartnerCompany(String id, UpdatePartnerCompanyDto updateDto) {
        log.info("파트너 회사 업데이트 요청 - ID: {}", id);

        PartnerCompany partnerCompany = partnerCompanyRepository.findByIdAndStatus(id, PartnerCompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        String.format("ID '%s'에 해당하는 파트너사를 찾을 수 없습니다.", id)));

        // 회사명이 변경되는 경우 중복 검사 및 복원 로직
        if (updateDto.getCompanyName() != null && 
            !updateDto.getCompanyName().equalsIgnoreCase(partnerCompany.getCompanyName())) {
            
            // 1. ACTIVE 상태의 중복 회사명 검사
            if (partnerCompanyRepository.existsByCompanyNameIgnoreCaseAndStatus(
                    updateDto.getCompanyName(), PartnerCompanyStatus.ACTIVE)) {
                log.warn("ACTIVE 상태의 중복된 회사명으로 파트너사 수정 시도 - 회사명: {}", updateDto.getCompanyName());
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    String.format("'%s' 이름의 파트너사가 이미 등록되어 있습니다.", updateDto.getCompanyName()));
            }
            
            // 2. INACTIVE 상태의 동일한 회사명이 있는지 확인하고 복원
            Optional<PartnerCompany> inactivePartner = partnerCompanyRepository
                    .findByCompanyNameIgnoreCaseAndStatus(updateDto.getCompanyName(), PartnerCompanyStatus.INACTIVE);
            
            if (inactivePartner.isPresent()) {
                log.info("INACTIVE 상태의 기존 파트너사 발견 - 복원 처리: {}", updateDto.getCompanyName());
                PartnerCompany existingPartner = inactivePartner.get();
                
                // 현재 파트너사는 삭제 처리
                partnerCompany.setStatus(PartnerCompanyStatus.INACTIVE);
                partnerCompany.setUpdatedAt(LocalDateTime.now());
                partnerCompanyRepository.save(partnerCompany);
                
                // 기존 데이터를 새로운 정보로 업데이트하고 ACTIVE로 복원
                existingPartner.setCorpCode(updateDto.getCorpCode() != null ? updateDto.getCorpCode() : partnerCompany.getCorpCode());
                existingPartner.setContractStartDate(updateDto.getContractStartDate() != null ? updateDto.getContractStartDate() : partnerCompany.getContractStartDate());
                existingPartner.setStatus(PartnerCompanyStatus.ACTIVE);
                existingPartner.setUpdatedAt(LocalDateTime.now());
                existingPartner.setMemberId(partnerCompany.getMemberId()); // 현재 사용자로 변경
                
                // 주식코드 업데이트
                String stockCode = updateDto.getStockCode();
                String finalCorpCode = updateDto.getCorpCode() != null ? updateDto.getCorpCode() : partnerCompany.getCorpCode();
                
                if (stockCode == null || stockCode.trim().isEmpty()) {
                    try {
                        CompanyProfileResponse dartProfile = dartApiService.getCompanyProfile(finalCorpCode).block();
                        if (dartProfile != null) {
                            stockCode = dartProfile.getStockCode();
                        }
                    } catch (Exception e) {
                        log.warn("DART API 호출 중 오류 발생: {}", e.getMessage());
                    }
                }
                existingPartner.setStockCode(stockCode);
                
                PartnerCompany restoredPartner = partnerCompanyRepository.save(existingPartner);
                log.info("파트너사 복원 완료 - ID: {}", restoredPartner.getId());
                
                // Kafka 메시지 발행 (복원 이벤트)
                try {
                    CompletableFuture<SendResult<String, Object>> future =
                            kafkaProducerService.sendPartnerRestoreEvent(restoredPartner.getId().toString(), mapToResponseDto(restoredPartner));
                    future.whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("파트너사 복원 이벤트 발행 성공: {}", restoredPartner.getId());
                        } else {
                            log.error("파트너사 복원 이벤트 발행 실패: {}", ex.getMessage());
                        }
                    });
                } catch (Exception e) {
                    log.error("파트너사 복원 이벤트 발행 중 오류: {}", e.getMessage());
                }
                
                return mapToResponseDto(restoredPartner, true); // 복원됨을 표시
            }
        }

        // 주식코드 업데이트 로직
        String stockCode = partnerCompany.getStockCode();
        String finalCorpCode = updateDto.getCorpCode() != null ? updateDto.getCorpCode() : partnerCompany.getCorpCode();

        // 프론트엔드에서 stockCode가 명시적으로 제공된 경우 우선 사용
        if (updateDto.getStockCode() != null) {
            stockCode = updateDto.getStockCode();
            log.info("프론트엔드에서 제공된 주식코드로 업데이트: {}", stockCode);
        }
        // corpCode가 변경된 경우 DART API에서 새로운 정보 조회
        else if (updateDto.getCorpCode() != null && !updateDto.getCorpCode().equals(partnerCompany.getCorpCode())) {
            try {
                CompanyProfileResponse dartProfile = dartApiService.getCompanyProfile(updateDto.getCorpCode()).block();
                if (dartProfile != null) {
                    log.info("DART API에서 회사 정보 조회 성공 - 회사명: {}", dartProfile.getCorpName());
                    stockCode = dartProfile.getStockCode();

                    // DART API에서 반환한 corpCode가 다른 경우 업데이트
                    if (dartProfile.getCorpCode() != null && !dartProfile.getCorpCode().equals(finalCorpCode)) {
                        log.info("DART API에서 반환한 corpCode({})가 요청한 corpCode({})와 다릅니다. DART API 값을 사용합니다.", 
                                dartProfile.getCorpCode(), finalCorpCode);
                        finalCorpCode = dartProfile.getCorpCode();
                    }
                }
            } catch (Exception e) {
                log.warn("DART API 호출 중 오류 발생: {}", e.getMessage());
            }
        }

        // 필드 업데이트
        if (updateDto.getCompanyName() != null) {
            partnerCompany.setCompanyName(updateDto.getCompanyName());
        }

        partnerCompany.setCorpCode(finalCorpCode);
        partnerCompany.setStockCode(stockCode);

        if (updateDto.getContractStartDate() != null) {
            partnerCompany.setContractStartDate(updateDto.getContractStartDate());
        }

        partnerCompany.setUpdatedAt(LocalDateTime.now());

        // 데이터베이스에 저장
        PartnerCompany updatedPartnerCompany = partnerCompanyRepository.save(partnerCompany);
        log.info("파트너 회사 업데이트 완료 - ID: {}", updatedPartnerCompany.getId());

        // 응답 DTO 생성
        PartnerCompanyResponseDto responseDto = mapToResponseDto(updatedPartnerCompany);

        // Kafka 메시지 발행
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaProducerService.sendMessage(partnerCompanyTopic, updatedPartnerCompany.getId().toString(), responseDto);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("파트너 회사 업데이트 메시지 발행 성공 - ID: {}", updatedPartnerCompany.getId());
                } else {
                    log.error("파트너 회사 업데이트 메시지 발행 실패 - ID: {}", updatedPartnerCompany.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 중 오류 발생", e);
        }

        return responseDto;
    }

    /**
     * 파트너 회사를 삭제(비활성화)합니다.
     * 
     * @param id 파트너 회사 ID
     * @return 삭제 결과 메시지
     * @throws ResponseStatusException 파트너 회사를 찾을 수 없는 경우
     */
    @Transactional
    public Map<String, String> deletePartnerCompany(String id) {
        log.info("파트너 회사 삭제 요청 - ID: {}", id);

        PartnerCompany partnerCompany = partnerCompanyRepository.findByIdAndStatus(id, PartnerCompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        String.format("ID '%s'에 해당하는 파트너사를 찾을 수 없습니다.", id)));

        // 상태를 INACTIVE로 변경 (소프트 삭제)
        partnerCompany.setStatus(PartnerCompanyStatus.INACTIVE);
        partnerCompany.setContractEndDate(LocalDateTime.now().toLocalDate());
        partnerCompany.setUpdatedAt(LocalDateTime.now());

        // 데이터베이스에 저장
        PartnerCompany deletedPartnerCompany = partnerCompanyRepository.save(partnerCompany);
        log.info("파트너 회사 삭제(비활성화) 완료 - ID: {}", deletedPartnerCompany.getId());

        // 응답 DTO 생성
        PartnerCompanyResponseDto responseDto = mapToResponseDto(deletedPartnerCompany);

        // Kafka 메시지 발행
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaProducerService.sendMessage(partnerCompanyTopic, deletedPartnerCompany.getId().toString(), responseDto);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("파트너 회사 삭제 메시지 발행 성공 - ID: {}", deletedPartnerCompany.getId());
                } else {
                    log.error("파트너 회사 삭제 메시지 발행 실패 - ID: {}", deletedPartnerCompany.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 중 오류 발생", e);
        }

        return Map.of("message",
                String.format("ID '%s' 파트너사가 성공적으로 비활성화되었습니다.", id));
    }

    /**
     * 파트너 회사 엔티티를 응답 DTO로 변환합니다.
     * 
     * @param partnerCompany 파트너 회사 엔티티
     * @return 파트너 회사 응답 DTO
     */
    private PartnerCompanyResponseDto mapToResponseDto(PartnerCompany partnerCompany) {
        if (partnerCompany == null) {
            return null;
        }

        return PartnerCompanyResponseDto.builder()
                .id(partnerCompany.getId())
                .corpCode(partnerCompany.getCorpCode())
                .corpName(partnerCompany.getCompanyName())
                .stockCode(partnerCompany.getStockCode())
                .contractStartDate(partnerCompany.getContractStartDate())
                .modifyDate(PartnerCompanyResponseDto.formatDateToYYYYMMDD(partnerCompany.getUpdatedAt()))
                .status(partnerCompany.getStatus())
                .industry(partnerCompany.getIndustry())
                .country(partnerCompany.getCountry())
                .address(partnerCompany.getAddress())
                .isRestored(false) // 기본값은 false (새로 생성됨)
                .build();
    }

    /**
     * 파트너 회사 엔티티를 응답 DTO로 변환합니다. (복원 플래그 포함)
     * 
     * @param partnerCompany 파트너 회사 엔티티
     * @param isRestored 복원 여부
     * @return 파트너 회사 응답 DTO
     */
    private PartnerCompanyResponseDto mapToResponseDto(PartnerCompany partnerCompany, boolean isRestored) {
        if (partnerCompany == null) {
            return null;
        }

        return PartnerCompanyResponseDto.builder()
                .id(partnerCompany.getId())
                .corpCode(partnerCompany.getCorpCode())
                .corpName(partnerCompany.getCompanyName())
                .stockCode(partnerCompany.getStockCode())
                .contractStartDate(partnerCompany.getContractStartDate())
                .modifyDate(PartnerCompanyResponseDto.formatDateToYYYYMMDD(partnerCompany.getUpdatedAt()))
                .status(partnerCompany.getStatus())
                .industry(partnerCompany.getIndustry())
                .country(partnerCompany.getCountry())
                .address(partnerCompany.getAddress())
                .isRestored(isRestored)
                .build();
    }

    /**
     * 특정 회원이 등록한 파트너 회사 목록을 조회합니다.
     * 
     * @param memberId 회원 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param pageSize 페이지당 항목 수
     * @param companyName 회사명 필터 (부분 일치)
     * @return 페이지네이션을 포함한 파트너 회사 목록
     */
    @Transactional(readOnly = true)
    public PaginatedPartnerCompanyResponseDto findAllPartnerCompaniesByMemberId(String memberId, int page, int pageSize, String companyName) {
        log.info("회원 ID '{}'의 파트너 회사 목록 조회 요청 - 페이지: {}, 페이지 크기: {}, 회사명 필터: {}", memberId, page, pageSize, companyName);

        // 페이지 번호 검증 및 보정
        int validPage = Math.max(1, page);
        int validPageSize = Math.max(1, Math.min(100, pageSize));
        
        log.debug("검증된 페이지 파라미터 - 원본: page={}, pageSize={} -> 보정: page={}, pageSize={}", 
                page, pageSize, validPage, validPageSize);

        // 페이지 번호는 0부터 시작하므로 1을 빼줌 (이제 항상 0 이상이 보장됨)
        Pageable pageable = PageRequest.of(validPage - 1, validPageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PartnerCompany> partnerCompaniesPage;
        if (companyName != null && !companyName.isEmpty()) {
            partnerCompaniesPage = partnerCompanyRepository.findByMemberIdAndCompanyNameContainingIgnoreCaseAndStatus(
                    memberId, companyName, PartnerCompanyStatus.ACTIVE, pageable);
        } else {
            partnerCompaniesPage = partnerCompanyRepository.findByMemberIdAndStatus(
                    memberId, PartnerCompanyStatus.ACTIVE, pageable);
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

    /**
     * 시스템에 등록된 모든 활성 상태의 파트너사들의 고유한 회사명 목록을 조회합니다.
     * 
     * @return 고유한 파트너사명 목록
     */
    @Transactional(readOnly = true)
    public List<String> getUniqueActivePartnerCompanyNames() {
        log.info("모든 활성 파트너사의 고유한 회사명 목록 조회 요청");
        List<PartnerCompany> activeCompanies = partnerCompanyRepository.findByStatus(PartnerCompanyStatus.ACTIVE);
        return activeCompanies.stream()
                .map(PartnerCompany::getCompanyName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Scope 등록용 협력사 목록을 조회합니다.
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param companyNameFilter 회사명 필터 (부분 일치)
     * @param includeInactive INACTIVE 협력사 포함 여부
     * @return 페이지네이션을 포함한 협력사 목록
     */
    @Transactional(readOnly = true)
    public PaginatedPartnerCompanyResponseDto getPartnerCompaniesForScope(int page, int size, String companyNameFilter, boolean includeInactive) {
        log.info("Scope용 협력사 목록 조회 - 페이지: {}, 크기: {}, 필터: {}, INACTIVE 포함: {}", page, size, companyNameFilter, includeInactive);

        // 페이지 파라미터 검증
        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(100, size));
        
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.ASC, "companyName"));

        Page<PartnerCompany> partnerCompaniesPage;
        
        if (includeInactive) {
            // ACTIVE와 INACTIVE 모두 포함
            if (companyNameFilter != null && !companyNameFilter.isEmpty()) {
                partnerCompaniesPage = partnerCompanyRepository.findByCompanyNameContainingIgnoreCase(companyNameFilter, pageable);
            } else {
                partnerCompaniesPage = partnerCompanyRepository.findAll(pageable);
            }
        } else {
            // ACTIVE만 포함
            if (companyNameFilter != null && !companyNameFilter.isEmpty()) {
                partnerCompaniesPage = partnerCompanyRepository.findByCompanyNameContainingIgnoreCaseAndStatus(
                        companyNameFilter, PartnerCompanyStatus.ACTIVE, pageable);
            } else {
                partnerCompaniesPage = partnerCompanyRepository.findByStatus(PartnerCompanyStatus.ACTIVE, pageable);
            }
        }

        List<PartnerCompanyResponseDto> partnerCompanies = partnerCompaniesPage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        return PaginatedPartnerCompanyResponseDto.builder()
                .data(partnerCompanies)
                .total(partnerCompaniesPage.getTotalElements())
                .page(validPage)
                .pageSize(validSize)
                .build();
    }

    /**
     * Scope용 특정 협력사 정보를 조회합니다. (INACTIVE 상태도 포함)
     * 
     * @param id 협력사 ID
     * @return 협력사 정보
     */
    @Transactional(readOnly = true)
    public PartnerCompanyResponseDto getPartnerCompanyForScope(String id) {
        log.info("Scope용 협력사 정보 조회 - ID: {}", id);

        try {

            PartnerCompany partnerCompany = partnerCompanyRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "ID '" + id + "'에 해당하는 협력사를 찾을 수 없습니다."));

            return mapToResponseDto(partnerCompany);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 UUID 형식: {}", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 ID 형식입니다.");
        }
    }

    /**
     * 협력사 회사명 중복 검사를 수행합니다.
     * 
     * @param companyName 검사할 회사명
     * @param excludeId 제외할 협력사 ID (수정 시 자기 자신 제외용)
     * @return 중복 검사 결과
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkCompanyNameDuplicate(String companyName, String excludeId) {
        log.info("협력사 회사명 중복 검사 - 회사명: {}, 제외 ID: {}", companyName, excludeId);
        
        if (companyName == null || companyName.trim().isEmpty()) {
            return Map.of(
                "isDuplicate", false,
                "message", "회사명이 제공되지 않았습니다."
            );
        }
        
        // 동일한 회사명을 가진 활성 상태의 협력사 검색
        Optional<PartnerCompany> existingCompany = partnerCompanyRepository
                .findByCompanyNameIgnoreCaseAndStatus(companyName.trim(), PartnerCompanyStatus.ACTIVE);
        
        boolean isDuplicate = false;
        String message = "";
        
        if (existingCompany.isPresent()) {
            // 수정 시 자기 자신은 제외
            if (excludeId != null && excludeId.equals(existingCompany.get().getId())) {
                isDuplicate = false;
                message = "현재 협력사와 동일한 회사명입니다.";
            } else {
                isDuplicate = true;
                message = String.format("'%s' 이름의 협력사가 이미 등록되어 있습니다.", companyName);
            }
        } else {
            isDuplicate = false;
            message = "사용 가능한 회사명입니다.";
        }
        
        return Map.of(
            "isDuplicate", isDuplicate,
            "message", message,
            "companyName", companyName
        );
    }
}
