/**
 * @file PartnerCompanyApiController.java
 * @description 파트너 회사 관련 CRUD 및 재무 위험 분석 기능을 제공하는 REST 컨트롤러입니다.
 *              파트너사 등록, 조회, 수정, 삭제(비활성화) 및 특정 파트너사의 재무 위험 분석 기능을 제공합니다.
 *              파트너사 등록 시 DTO 필드 변경 사항이 반영되었습니다. (contractEndDate, industry, country, address 제거)
 */
package com.nsmm.esg.dart_service.partner.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nsmm.esg.dart_service.partner.dto.CreatePartnerCompanyDto;
import com.nsmm.esg.dart_service.partner.dto.PaginatedPartnerCompanyResponseDto;
import com.nsmm.esg.dart_service.partner.dto.PartnerCompanyResponseDto;
import com.nsmm.esg.dart_service.partner.dto.UpdatePartnerCompanyDto;
import com.nsmm.esg.dart_service.partner.dto.FinancialRiskAssessmentDto;
import com.nsmm.esg.dart_service.partner.service.PartnerCompanyApiService;
import com.nsmm.esg.dart_service.partner.service.PartnerFinancialRiskService;
import com.nsmm.esg.dart_service.kafka.service.KafkaProducerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RestController
@RequestMapping("/partners")
@Tag(name = "파트너 회사 API", description = "파트너 회사 API 정보를 제공하는 API")
@RequiredArgsConstructor
@Slf4j
public class PartnerCompanyApiController {

        private final PartnerCompanyApiService partnerCompanyApiService;
        private final PartnerFinancialRiskService partnerFinancialRiskService;
        private final KafkaProducerService kafkaProducerService;

        @Value("${kafka.topic.news-keywords}")
        private String newsKeywordsTopic;

        @GetMapping("/companies/{companyId}")
        @Operation(summary = "파트너사 외부 시스템 회사 정보 조회", description = "파트너사 외부 시스템 API를 통해 특정 회사 정보를 조회합니다. (주의: 현재 서비스의 파트너사 DB와는 별개)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "회사 정보 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))),
                        @ApiResponse(responseCode = "404", description = "외부 시스템에서 해당 회사 정보를 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "외부 시스템 API 호출 오류 또는 서버 내부 오류")
        })
        public ResponseEntity<Map<String, Object>> getCompanyInfo(
                        @Parameter(description = "조회할 회사의 외부 시스템 ID", required = true, example = "external-company-123") @PathVariable String companyId) {

                log.info("파트너 회사 정보 조회 API 요청 - 회사 ID: {}", companyId);
                Map<String, Object> response = partnerCompanyApiService.getCompanyInfo(companyId);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/companies/{companyId}/financials")
        @Operation(summary = "파트너사 외부 시스템 재무 정보 조회", description = "파트너사 외부 시스템 API를 통해 특정 회사의 특정 연도, 분기 재무 정보를 조회합니다. (주의: 현재 서비스의 파트너사 DB와는 별개)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "재무 정보 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터 (예: 유효하지 않은 연도 또는 분기)"),
                        @ApiResponse(responseCode = "404", description = "외부 시스템에서 해당 재무 정보를 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "외부 시스템 API 호출 오류 또는 서버 내부 오류")
        })
        public ResponseEntity<Map<String, Object>> getFinancialInfo(
                        @Parameter(description = "조회할 회사의 외부 시스템 ID", required = true, example = "external-company-123") @PathVariable String companyId,

                        @Parameter(description = "조회 연도 (YYYY 형식)", required = true, example = "2023") @RequestParam int year,

                        @Parameter(description = "조회 분기 (1, 2, 3, 4 중 하나)", required = true, example = "1") @RequestParam int quarter) {

                log.info("파트너 회사 재무 정보 조회 API 요청 - 회사 ID: {}, {}년 {}분기", companyId, year, quarter);
                Map<String, Object> response = partnerCompanyApiService.getFinancialInfo(companyId, year, quarter);
                return ResponseEntity.ok(response);
        }

        // 파트너 회사 CRUD 엔드포인트

        @PostMapping("/partner-companies")
        @Operation(summary = "신규 파트너사 등록", description = "새로운 파트너사를 시스템에 등록합니다. 등록 시 DART API를 통해 추가 정보를 조회하여 저장하며, Kafka로 파트너사 등록 이벤트를 발행합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "파트너사가 성공적으로 등록되었습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (예: 필수 필드 누락, 형식 오류)"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 DART API 연동 오류")
        })
        public ResponseEntity<PartnerCompanyResponseDto> createPartnerCompany(
                        @Parameter(description = "파트너사를 등록하는 회원의 ID (요청 헤더 X-Member-Id로 전달)", required = true, example = "user-member-uuid") @RequestHeader("X-MEMBER-ID") String memberId,

                        @Parameter(description = "등록할 파트너사의 정보", required = true, schema = @Schema(implementation = CreatePartnerCompanyDto.class)) @Valid @RequestBody CreatePartnerCompanyDto createDto) {

                log.info("파트너사 등록 API 요청 - 회사명: {}, 회원 ID: {}", createDto.getCompanyName(), memberId);
                PartnerCompanyResponseDto response = partnerCompanyApiService.createPartnerCompany(createDto, memberId);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/partner-companies")
        @Operation(summary = "특정 사용자의 파트너사 목록 조회 (페이지네이션)", description = "X-Member-Id 헤더로 전달된 사용자가 등록한 활성(ACTIVE) 상태의 파트너사 목록을 페이지네이션하여 조회합니다. 회사명으로 필터링할 수 있습니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "페이지네이션을 포함한 파트너사 목록입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedPartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 페이지네이션 파라미터 또는 X-Member-Id 헤더 누락"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<PaginatedPartnerCompanyResponseDto> findAllPartnerCompanies(
                        @Parameter(description = "파트너사를 조회하는 회원의 ID (요청 헤더 X-Member-Id로 전달)", required = true, example = "user-member-uuid") @RequestHeader("X-MEMBER-ID") String memberId,

                        @Parameter(description = "조회할 페이지 번호 (1부터 시작)", example = "1") @RequestParam(defaultValue = "1") int page,

                        @Parameter(description = "페이지당 표시할 항목 수", example = "10") @RequestParam(defaultValue = "10") int pageSize,

                        @Parameter(description = "검색할 회사명 (부분 일치, 대소문자 구분 없음)") @RequestParam(required = false) String companyName) {

                // 페이지 파라미터 검증
                int validPage = Math.max(1, page);
                int validPageSize = Math.max(1, Math.min(100, pageSize));

                log.info("파트너사 목록 조회 API 요청 - 회원 ID: {}, 페이지: {} (검증후: {}), 페이지 크기: {} (검증후: {}), 회사명 필터: {}",
                                memberId, page, validPage, pageSize, validPageSize, companyName);

                PaginatedPartnerCompanyResponseDto response = partnerCompanyApiService
                                .findAllPartnerCompaniesByMemberId(memberId, validPage, validPageSize, companyName);
                return ResponseEntity.ok(response);
        }

        // --------------------------------------------------------------------------------------------------------------------------------------------

        @GetMapping("/unique-partner-companies")
        @Operation(summary = "모든 고유 파트너사명 목록 조회", description = "시스템에 등록된 모든 활성(ACTIVE) 상태의 파트너사들의 고유한 회사명 목록을 조회합니다. 사용자 ID와 무관합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "고유한 파트너사명 목록입니다.", content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = String.class))),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<java.util.List<String>> getUniquePartnerCompanyNames() {
                log.info("고유 파트너사명 목록 조회 API 요청");
                java.util.List<String> response = partnerCompanyApiService.getUniqueActivePartnerCompanyNames();
                return ResponseEntity.ok(response);
        }
        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        @GetMapping("/partner-companies/{id}")
        @Operation(summary = "특정 파트너사 상세 조회 (ID)", description = "시스템에 등록된 특정 파트너사의 상세 정보를 ID(UUID)를 이용하여 조회합니다. 활성(ACTIVE) 상태의 파트너사만 조회됩니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "파트너사 상세 정보입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "404", description = "요청한 ID에 해당하는 활성 파트너사를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<PartnerCompanyResponseDto> findPartnerCompanyById(
                        @Parameter(description = "조회할 파트너사의 고유 ID (UUID 형식)", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") @PathVariable String id) {

                log.info("파트너사 상세 조회 API 요청 - ID: {}", id);
                PartnerCompanyResponseDto response = partnerCompanyApiService.findPartnerCompanyById(id);
                return ResponseEntity.ok(response);
        }
        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        @PatchMapping("/partner-companies/{id}")
        @Operation(summary = "특정 파트너사 정보 수정 (ID)", description = "시스템에 등록된 특정 파트너사의 정보를 ID(UUID)를 이용하여 수정합니다. corpCode 변경 시 DART API를 통해 추가 정보를 업데이트하며, Kafka로 파트너사 업데이트 이벤트를 발행합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "파트너사 정보가 성공적으로 수정되었습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (예: 형식 오류)"),
                        @ApiResponse(responseCode = "404", description = "수정할 파트너사를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 DART API 연동 오류")
        })
        public ResponseEntity<PartnerCompanyResponseDto> updatePartnerCompany(
                        @RequestHeader("X-MEMBER-ID") String memberId,
                        @Parameter(description = "수정할 파트너사의 고유 ID (UUID 형식)", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") @PathVariable String id,

                        @Parameter(description = "수정할 파트너사의 정보", required = true, schema = @Schema(implementation = UpdatePartnerCompanyDto.class)) @Valid @RequestBody UpdatePartnerCompanyDto updateDto) {

                log.info("파트너사 정보 수정 API 요청 - ID: {}, 사용자 ID: {}", id, memberId);
                PartnerCompanyResponseDto response = partnerCompanyApiService.updatePartnerCompany(id, updateDto);
                return ResponseEntity.ok(response);
        }
        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        @DeleteMapping("/partner-companies/{id}")
        @Operation(summary = "특정 파트너사 삭제 (ID, 소프트 삭제)", description = "시스템에 등록된 특정 파트너사를 논리적으로 삭제합니다 (상태를 INACTIVE로 변경). Kafka로 파트너사 삭제 이벤트를 발행합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "파트너사가 성공적으로 비활성화(소프트 삭제)되었습니다.", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"message\": \"ID '...' 파트너사가 성공적으로 비활성화되었습니다.\"}"))),
                        @ApiResponse(responseCode = "404", description = "삭제할 파트너사를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<Map<String, String>> deletePartnerCompany(
                        @RequestHeader("X-MEMBER-ID") String memberId,
                        @Parameter(description = "삭제(비활성화)할 파트너사의 고유 ID (UUID 형식)", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") @PathVariable String id) {

                log.info("파트너사 삭제 API 요청 - ID: {}, 사용자 ID: {}", id, memberId);
                Map<String, String> response = partnerCompanyApiService.deletePartnerCompany(id);
                return ResponseEntity.ok(response);
        }
        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        /**
         * 파트너사 재무 위험 분석 (DB 기반)
         */
        @GetMapping("/partner-companies/{partnerCorpCode}/financial-risk")
        @Operation(summary = "파트너사 재무 위험 분석 (DB 기반)", description = "내부 데이터베이스에 저장된 특정 파트너사의 재무제표 데이터를 기반으로 최근 4분기(1년) 기준으로 재무 위험을 분석합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "재무 위험 분석 결과입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinancialRiskAssessmentDto.class))),
                        @ApiResponse(responseCode = "404", description = "파트너사 또는 해당 조건의 재무 데이터를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 분석 중 오류 발생")
        })
        public ResponseEntity<FinancialRiskAssessmentDto> getFinancialRiskAssessment(
                        @Parameter(description = "재무 위험을 분석할 파트너사의 DART 고유번호 (8자리 숫자)", required = true, example = "00126380") @PathVariable String partnerCorpCode,
                        @Parameter(description = "파트너사명 (결과 표시에 사용, 필수는 아님)") @RequestParam(required = false) String partnerName) {

                log.info("파트너사 재무 위험 분석 요청 - corpCode: {}, partnerName: {}", partnerCorpCode, partnerName);

                try {
                        FinancialRiskAssessmentDto assessment = partnerFinancialRiskService
                                        .assessFinancialRisk(partnerCorpCode, partnerName);
                        return ResponseEntity.ok(assessment);
                } catch (Exception e) {
                        log.error("파트너사 재무 위험 분석 중 오류 발생 - corpCode: {}", partnerCorpCode, e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "재무 위험 분석 중 오류가 발생했습니다: " + e.getMessage());
                }
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
        // 스코프 설정용 파트너 회사 목록 조회 컨트롤러

        @GetMapping("/partner-companies/for-scope")
        @Operation(summary = "Scope 등록용 협력사 목록 조회", description = "Scope 데이터 등록 시 사용하는 협력사 목록을 조회합니다. ACTIVE 상태 협력사만 반환하며, 이미 등록된 Scope의 조회 시에는 INACTIVE 협력사 정보도 포함됩니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Scope용 협력사 목록 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedPartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<PaginatedPartnerCompanyResponseDto> getPartnerCompaniesForScope(
                        @RequestHeader("X-MEMBER-ID") String memberId,
                        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "페이지 크기", example = "100") @RequestParam(defaultValue = "100") int size,
                        @Parameter(description = "회사명 필터 (부분 검색)", required = false) @RequestParam(required = false) String companyNameFilter,
                        @Parameter(description = "INACTIVE 협력사 포함 여부 (기본값: false)", example = "false") @RequestParam(defaultValue = "false") boolean includeInactive) {

                log.info("Scope용 협력사 목록 조회 API 요청 - 사용자 ID: {}, 페이지: {}, 크기: {}, 필터: {}, INACTIVE 포함: {}",
                                memberId, page, size, companyNameFilter, includeInactive);

                PaginatedPartnerCompanyResponseDto response = partnerCompanyApiService.getPartnerCompaniesForScope(
                                page, size, companyNameFilter, includeInactive);
                return ResponseEntity.ok(response);
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
        // 스코프 설정용 파트너 회사 상세 정보 조회 컨트롤러
        @GetMapping("/partner-companies/{id}/for-scope")
        @Operation(summary = "Scope용 특정 협력사 정보 조회", description = "Scope 데이터에서 사용하는 특정 협력사 정보를 조회합니다. INACTIVE 상태 협력사도 조회 가능합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "협력사 정보 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "404", description = "협력사를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<PartnerCompanyResponseDto> getPartnerCompanyForScope(
                        @RequestHeader("X-MEMBER-ID") String memberId,
                        @Parameter(description = "조회할 협력사의 고유 ID (UUID 형식)", required = true) @PathVariable String id) {

                log.info("Scope용 협력사 정보 조회 API 요청 - ID: {}, 사용자 ID: {}", id, memberId);
                PartnerCompanyResponseDto response = partnerCompanyApiService.getPartnerCompanyForScope(id);
                return ResponseEntity.ok(response);
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
        // 파트너 회사명 중복 확인 컨트롤러
        @GetMapping("/partner-companies/check-duplicate")
        @Operation(summary = "협력사 회사명 중복 검사", description = "새로운 협력사 등록 또는 기존 협력사 수정 시 회사명 중복 여부를 확인합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "중복 검사 완료", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<Map<String, Object>> checkCompanyNameDuplicate(
                        @RequestHeader("X-MEMBER-ID") String memberId,
                        @Parameter(description = "검사할 회사명", required = true) @RequestParam String companyName,
                        @Parameter(description = "수정 시 제외할 협력사 ID (새 등록 시 생략)", required = false) @RequestParam(required = false) String excludeId) {

                log.info("협력사 회사명 중복 검사 API 요청 - 사용자 ID: {}, 회사명: {}, 제외 ID: {}",
                                memberId, companyName, excludeId);

                Map<String, Object> response = partnerCompanyApiService.checkCompanyNameDuplicate(companyName,
                                excludeId);
                return ResponseEntity.ok(response);
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
}
