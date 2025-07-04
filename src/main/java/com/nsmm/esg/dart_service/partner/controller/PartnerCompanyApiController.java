/**
 * @file PartnerCompanyApiController.java
 * @description 협력사 관련 CRUD 및 재무 위험 분석 기능을 제공하는 REST 컨트롤러입니다.
 *              협력사 등록, 조회, 수정, 삭제(비활성화) 및 특정 협력\사의 재무 위험 분석 기능을 제공합니다.
 *              협력사 등록 시 DTO 필드 변경 사항이 반영되었습니다. (contractEndDate, industry, country, address 제거)
 */
package com.nsmm.esg.dart_service.partner.controller;
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
import com.nsmm.esg.dart_service.partner.dto.AvailablePeriodDto;
import com.nsmm.esg.dart_service.partner.service.PartnerCompanyApiService;
import com.nsmm.esg.dart_service.partner.service.PartnerFinancialRiskService;

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
@Tag(name = "협력사 API", description = "협력 회사 API 정보를 제공하는 API")
@RequiredArgsConstructor
@Slf4j
public class PartnerCompanyApiController {

        private final PartnerCompanyApiService partnerCompanyApiService;
        private final PartnerFinancialRiskService partnerFinancialRiskService;

        // 협력사 외부 시스템 회사 정보 조회
        @GetMapping("/companies/{companyId}")
        @Operation(summary = "협력사 외부 시스템 회사 정보 조회", description = "협력사 외부 시스템 API를 통해 특정 회사 정보를 조회합니다. (주의: 현재 서비스의 협력사 DB와는 별개)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "회사 정보 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))),
                        @ApiResponse(responseCode = "404", description = "외부 시스템에서 해당 회사 정보를 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "외부 시스템 API 호출 오류 또는 서버 내부 오류")
        })
        public ResponseEntity<Map<String, Object>> getCompanyInfo(
                        @Parameter(description = "조회할 회사의 외부 시스템 ID", required = true, example = "external-company-123") @PathVariable String companyId) {

                log.info("협력 회사 정보 조회 API 요청 - 회사 ID: {}", companyId);
                Map<String, Object> response = partnerCompanyApiService.getCompanyInfo(companyId);
                return ResponseEntity.ok(response);
        }
        // --------------------------------------------------------------------------------------------------------------------------------------------

        // 협력사 외부 시스템 재무 정보 조회
        @GetMapping("/companies/{companyId}/financials")
        @Operation(summary = "협력사 외부 시스템 재무 정보 조회", description = "협력사 외부 시스템 API를 통해 특정 회사의 특정 연도, 분기 재무 정보를 조회합니다. (주의: 현재 서비스의 협력사 DB와는 별개)")
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

                log.info("협력 회사 재무 정보 조회 API 요청 - 회사 ID: {}, {}년 {}분기", companyId, year, quarter);
                Map<String, Object> response = partnerCompanyApiService.getFinancialInfo(companyId, year, quarter);
                return ResponseEntity.ok(response);
        }
        // --------------------------------------------------------------------------------------------------------------------------------------------

        // 새로운 협력사 추가
        @PostMapping("/partner-companies")
        @Operation(summary = "신규 협력사 등록", description = "새로운 협력사를 시스템에 등록합니다. 등록 시 DART API를 통해 추가 정보를 조회하여 저장하며, Kafka로 협력사 등록 이벤트를 발행합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "협력사가 성공적으로 등록되었습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (예: 필수 필드 누락, 형식 오류)"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 DART API 연동 오류")
        })
        public ResponseEntity<PartnerCompanyResponseDto> createPartnerCompany(
                        @Parameter(description = "본사 ID (게이트웨이 헤더)", required = true) @RequestHeader("X-HEADQUARTERS-ID") Long headquartersId,
                        @Parameter(description = "협력사 사용자 ID (게이트웨이 헤더, 선택사항)") @RequestHeader(value = "X-PARTNER-ID", required = false) Long partnerId,
                        @Parameter(description = "본사 계정 번호 (게이트웨이 헤더)", required = true) @RequestHeader("X-ACCOUNT-NUMBER") String hqAccountNumber,

                        @Parameter(description = "등록할 협력사의 정보", required = true, schema = @Schema(implementation = CreatePartnerCompanyDto.class)) @Valid @RequestBody CreatePartnerCompanyDto createDto) {

                log.info("협력사 등록 API 요청 - 회사명: {}, 본사 ID: {}, 협력사 ID: {}", createDto.getCompanyName(), headquartersId,
                                partnerId);
                PartnerCompanyResponseDto response = partnerCompanyApiService.createPartnerCompany(createDto,
                                headquartersId, partnerId, hqAccountNumber);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        // --------------------------------------------------------------------------------------------------------------------------------------------

        // 협력사 목록 조회 (페이지네이션)
        @GetMapping("/partner-companies")
        @Operation(summary = "조직별 협력사 목록 조회 (페이지네이션)", description = "본사/협력사별로 등록한 활성(ACTIVE) 상태의 협력사 목록을 페이지네이션하여 조회합니다. 회사명으로 필터링할 수 있습니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "페이지네이션을 포함한 협력사 목록입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedPartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 페이지네이션 파라미터 또는 필수 헤더 누락"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<PaginatedPartnerCompanyResponseDto> findAllPartnerCompanies(
                        @Parameter(description = "본사 ID (게이트웨이 헤더)", required = true) @RequestHeader("X-HEADQUARTERS-ID") Long headquartersId,
                        @Parameter(description = "협력사 사용자 ID (게이트웨이 헤더, 선택사항)") @RequestHeader(value = "X-PARTNER-ID", required = false) Long partnerId,

                        @Parameter(description = "조회할 페이지 번호 (1부터 시작)", example = "1") @RequestParam(defaultValue = "1") int page,

                        @Parameter(description = "페이지당 표시할 항목 수", example = "10") @RequestParam(defaultValue = "10") int pageSize,

                        @Parameter(description = "검색할 회사명 (부분 일치, 대소문자 구분 없음)") @RequestParam(required = false) String companyName) {

                // 페이지 파라미터 검증
                int validPage = Math.max(1, page);
                int validPageSize = Math.max(1, Math.min(100, pageSize));

                log.info("협력사 목록 조회 API 요청 - 본사 ID: {}, 협력사 ID: {}, 페이지: {} (검증후: {}), 페이지 크기: {} (검증후: {}), 회사명 필터: {}",
                                headquartersId, partnerId, page, validPage, pageSize, validPageSize, companyName);

                PaginatedPartnerCompanyResponseDto response = partnerCompanyApiService
                                .findAllPartnerCompaniesByOrganization(headquartersId, partnerId, validPage,
                                                validPageSize, companyName);
                return ResponseEntity.ok(response);
        }
        // --------------------------------------------------------------------------------------------------------------------------------------------

        // 모든 고유 협력사명 목록 조회
        @GetMapping("/unique-partner-companies")
        @Operation(summary = "모든 고유 협력사명 목록 조회", description = "시스템에 등록된 모든 활성(ACTIVE) 상태의 협력사들의 고유한 회사명 목록을 조회합니다. 사용자 ID와 무관합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "고유한 협력사명 목록입니다.", content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = String.class))),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<java.util.List<String>> getUniquePartnerCompanyNames() {
                log.info("고유 협력사명 목록 조회 API 요청");
                java.util.List<String> response = partnerCompanyApiService.getUniqueActivePartnerCompanyNames();
                return ResponseEntity.ok(response);
        }
        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // 특정 협력사 상세 조회 (ID)
        @GetMapping("/partner-companies/{id}")
        @Operation(summary = "특정 협력사 상세 조회 (ID)", description = "시스템에 등록된 특정 협력사의 상세 정보를 ID(UUID)를 이용하여 조회합니다. 활성(ACTIVE) 상태의 협력사만 조회됩니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "협력사 상세 정보입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "404", description = "요청한 ID에 해당하는 활성 협력사를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<PartnerCompanyResponseDto> findPartnerCompanyById(
                        @Parameter(description = "조회할 협력사의 고유 ID (UUID 형식)", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") @PathVariable String id) {

                log.info("협력사 상세 조회 API 요청 - ID: {}", id);
                PartnerCompanyResponseDto response = partnerCompanyApiService.findPartnerCompanyById(id);
                return ResponseEntity.ok(response);
        }
        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // 특정 협력사 정보 수정 (ID)
        @PatchMapping("/partner-companies/{id}")
        @Operation(summary = "특정 협력사 정보 수정 (ID)", description = "시스템에 등록된 특정 협력사의 정보를 ID(UUID)를 이용하여 수정합니다. 계약 관련 정보만 수정 가능합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "협력사 정보가 성공적으로 수정되었습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PartnerCompanyResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (예: 형식 오류)"),
                        @ApiResponse(responseCode = "404", description = "수정할 협력사를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<PartnerCompanyResponseDto> updatePartnerCompany(
                        @Parameter(description = "본사 ID (게이트웨이 헤더)", required = true) @RequestHeader("X-HEADQUARTERS-ID") Long headquartersId,
                        @Parameter(description = "협력사 사용자 ID (게이트웨이 헤더, 선택사항)") @RequestHeader(value = "X-PARTNER-ID", required = false) Long partnerId,
                        @Parameter(description = "수정할 협력사의 고유 ID (UUID 형식)", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") @PathVariable String id,

                        @Parameter(description = "수정할 협력사의 정보", required = true, schema = @Schema(implementation = UpdatePartnerCompanyDto.class)) @Valid @RequestBody UpdatePartnerCompanyDto updateDto) {

                log.info("협력사 정보 수정 API 요청 - ID: {}, 본사 ID: {}, 협력사 ID: {}", id, headquartersId, partnerId);
                PartnerCompanyResponseDto response = partnerCompanyApiService.updatePartnerCompany(id, updateDto);
                return ResponseEntity.ok(response);
        }
        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // 특정 협력사 삭제 (소프트 삭제)
        @DeleteMapping("/partner-companies/{id}")
        @Operation(summary = "특정 협력사 삭제 (ID, 소프트 삭제)", description = "시스템에 등록된 특정 협력사를 논리적으로 삭제합니다 (상태를 INACTIVE로 변경).")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "협력사가 성공적으로 비활성화(소프트 삭제)되었습니다.", content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"message\": \"협력사가 성공적으로 비활성화되었습니다.\", \"id\": \"uuid\"}"))),
                        @ApiResponse(responseCode = "404", description = "삭제할 협력사를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<Map<String, String>> deletePartnerCompany(
                        @Parameter(description = "본사 ID (게이트웨이 헤더)", required = true) @RequestHeader("X-HEADQUARTERS-ID") Long headquartersId,
                        @Parameter(description = "협력사 사용자 ID (게이트웨이 헤더, 선택사항)") @RequestHeader(value = "X-PARTNER-ID", required = false) Long partnerId,
                        @Parameter(description = "삭제(비활성화)할 협력사의 고유 ID (UUID 형식)", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") @PathVariable String id) {

                log.info("협력사 삭제 API 요청 - ID: {}, 본사 ID: {}, 협력사 ID: {}", id, headquartersId, partnerId);
                Map<String, String> response = partnerCompanyApiService.deletePartnerCompany(id);
                return ResponseEntity.ok(response);
        }
        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // 협력사 재무 위험 분석 (DB 기반)
        @GetMapping("/partner-companies/{partnerCorpCode}/financial-risk")
        @Operation(summary = "협력사 재무 위험 분석 (DB 기반)", description = "내부 데이터베이스에 저장된 특정 협력사의 재무제표 데이터를 기반으로 재무 위험을 분석합니다. 연도와 보고서 코드를 지정하지 않으면 최근 공시 기준으로 자동 선택됩니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "재무 위험 분석 결과입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FinancialRiskAssessmentDto.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터 (예: 유효하지 않은 연도 또는 보고서 코드)"),
                        @ApiResponse(responseCode = "404", description = "협력사 또는 해당 조건의 재무 데이터를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류 또는 분석 중 오류 발생")
        })
        public ResponseEntity<FinancialRiskAssessmentDto> getFinancialRiskAssessment(
                        @Parameter(description = "재무 위험을 분석할 협력사의 DART 고유번호 (8자리 숫자)", required = true, example = "00126380") @PathVariable String partnerCorpCode,

                        @Parameter(description = "협력사명 (결과 표시에 사용, 필수는 아님)") @RequestParam(required = false) String partnerName,

                        @Parameter(description = "분석할 사업연도 (YYYY 형식, 미지정시 자동 선택)", example = "2023") @RequestParam(required = false) String bsnsYear,

                        @Parameter(description = "분석할 보고서 코드 (11011: 사업보고서, 11012: 반기보고서, 11013: 1분기보고서, 11014: 3분기보고서, 미지정시 자동 선택)", example = "11011") @RequestParam(required = false) String reprtCode) {

                log.info("협력사 재무 위험 분석 요청 - corpCode: {}, partnerName: {}, bsnsYear: {}, reprtCode: {}",
                                partnerCorpCode, partnerName, bsnsYear, reprtCode);
                log.info("파라미터 상세 - bsnsYear null 여부: {}, reprtCode null 여부: {}", bsnsYear == null, reprtCode == null);

                try {
                        FinancialRiskAssessmentDto assessment;

                        // 연도나 보고서 코드가 지정된 경우 수동 분석, 아니면 자동 분석
                        if (bsnsYear != null || reprtCode != null) {
                                // 입력 검증 (값이 있는 경우만)
                                if (bsnsYear != null && !isValidBsnsYear(bsnsYear)) {
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "잘못된 사업연도 형식입니다. YYYY 형식으로 입력해주세요. (예: 2023)");
                                }
                                if (reprtCode != null && !isValidReprtCode(reprtCode)) {
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                        "잘못된 보고서 코드입니다. 11011, 11012, 11013, 11014 중 하나를 입력해주세요.");
                                }

                                log.info("수동 지정 모드: 사업연도={}, 보고서코드={}", bsnsYear, reprtCode);
                                assessment = partnerFinancialRiskService.assessFinancialRisk(partnerCorpCode,
                                                partnerName, bsnsYear, reprtCode);
                        } else {
                                log.info("자동 선택 모드: 최신 공시 기준으로 분석");
                                assessment = partnerFinancialRiskService.assessFinancialRisk(partnerCorpCode,
                                                partnerName);
                        }

                        return ResponseEntity.ok(assessment);
                } catch (ResponseStatusException e) {
                        throw e; // 이미 적절한 HTTP 상태 코드가 설정된 예외는 그대로 전파
                } catch (Exception e) {
                        log.error("협력사 재무 위험 분석 중 오류 발생 - corpCode: {}", partnerCorpCode, e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "재무 위험 분석 중 오류가 발생했습니다: " + e.getMessage());
                }
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // 협력 회사명 중복 확인 컨트롤러
        @GetMapping("/partner-companies/check-duplicate")
        @Operation(summary = "협력사 회사명 중복 검사", description = "새로운 협력사 등록 또는 기존 협력사 수정 시 회사명 중복 여부를 확인합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "중복 검사 완료", content = @Content(mediaType = "application/json", schema = @Schema(type = "object"))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<Map<String, Object>> checkCompanyNameDuplicate(
                        @Parameter(description = "검사할 회사명", required = true) @RequestParam String companyName,
                        @Parameter(description = "수정 시 제외할 협력사 ID (새 등록 시 생략)", required = false) @RequestParam(required = false) String excludeId) {

                log.info("협력사 회사명 중복 검사 API 요청 - 회사명: {}, 제외 ID: {}",
                                companyName, excludeId);

                Map<String, Object> response = partnerCompanyApiService.checkCompanyNameDuplicate(companyName,
                                excludeId);
                return ResponseEntity.ok(response);
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // 특정 협력사의 재무제표 데이터가 존재하는 연도/분기 조합 목록 조회
        @GetMapping("/partner-companies/{partnerCorpCode}/available-periods")
        @Operation(summary = "협력사 재무제표 이용 가능 기간 조회", description = "특정 협력사의 DB에 저장된 재무제표 데이터가 존재하는 연도/분기 조합 목록을 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "이용 가능한 기간 목록입니다.", content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = AvailablePeriodDto.class))),
                        @ApiResponse(responseCode = "404", description = "협력사의 재무제표 데이터를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<java.util.List<AvailablePeriodDto>> getAvailablePeriods(
                        @Parameter(description = "조회할 협력사의 DART 고유번호 (8자리 숫자)", required = true, example = "00126380") @PathVariable String partnerCorpCode) {

                log.info("협력사 이용 가능 기간 조회 API 요청 - corpCode: {}", partnerCorpCode);
                java.util.List<AvailablePeriodDto> response = partnerFinancialRiskService
                                .getAvailablePeriods(partnerCorpCode);
                return ResponseEntity.ok(response);
        }

        // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

        // 협력사 계정 생성 상태 변경 (accountCreated)
        @PatchMapping("/partner-companies/{id}/account-created")
        @Operation(summary = "계정 생성 상태 변경", description = "특정 협력사의 accountCreated 값을 true로 변경합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "계정 생성 상태가 성공적으로 변경되었습니다."),
                        @ApiResponse(responseCode = "404", description = "해당 ID의 협력사를 찾을 수 없습니다."),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<Void> setAccountCreatedTrue(@PathVariable String id) {
                log.info("협력사 계정 생성 상태 변경 요청 - ID: {}", id);
                partnerCompanyApiService.updateAccountCreatedStatus(id, true);
                return ResponseEntity.ok().build();
        }
        // --------------------------------------------------------------------------------------------------------------------------------------------

        // 사업연도 형식 검증 (YYYY)
        private boolean isValidBsnsYear(String bsnsYear) {
                if (bsnsYear == null || bsnsYear.length() != 4) {
                        return false;
                }
                try {
                        int year = Integer.parseInt(bsnsYear);
                        return year >= 2000 && year <= 2030; // 합리적인 범위 내
                } catch (NumberFormatException e) {
                        return false;
                }
        }

        // 보고서 코드 검증 (11011, 11012, 11013, 11014 중 하나)
        private boolean isValidReprtCode(String reprtCode) {
                return reprtCode != null &&
                        ("11011".equals(reprtCode) || "11012".equals(reprtCode) ||
                                "11013".equals(reprtCode) || "11014".equals(reprtCode));
        }

}
