/**
 * @file PartnerCompanyResponseDto.java
 * @description 파트너사 조회 시 반환되는 응답 DTO입니다.
 *              PartnerCompany 엔티티의 정보와 연관된 CompanyProfile의 회사 상세 정보를 포함합니다.
 */
package com.nsmm.esg.dart_service.partner.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.nsmm.esg.dart_service.partner.model.PartnerCompanyStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "파트너사 조회 시 반환되는 응답 DTO. CompanyProfile의 회사 정보와 계약 정보를 포함합니다.")
public class PartnerCompanyResponseDto {

    // ====================================================================
    // PartnerCompany 기본 정보
    // ====================================================================

    @Schema(description = "파트너사 고유 ID (UUID)", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String id;

    @Schema(description = "DART 기업 고유 코드 (8자리)", example = "00126380")
    private String corpCode;

    @Schema(description = "파트너사 상태", example = "ACTIVE")
    private PartnerCompanyStatus status;

    @Schema(description = "계약 시작일", example = "2023-01-01")
    private LocalDate contractStartDate;

    @Schema(description = "계약 종료일", example = "2024-12-31")
    private LocalDate contractEndDate;

    @Schema(description = "파트너사 등록 일시", example = "2023-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "파트너사 정보 최종 수정 일시", example = "2023-01-01T10:00:00")
    private LocalDateTime updatedAt;

    // ====================================================================
    // 권한 및 계층형 구조 정보
    // ====================================================================

    @Schema(description = "본사 ID", example = "1")
    private Long headquartersId;

    @Schema(description = "등록한 협력사 ID (협력사가 등록한 경우)", example = "2")
    private Long partnerId;

    @Schema(description = "본사 계정 번호", example = "HQ001")
    private String hqAccountNumber;

    @Schema(description = "계층적 ID", example = "L1-001")
    private String hierarchicalId;

    @Schema(description = "협력사 계층 레벨 (1차=1, 2차=2, ...)", example = "1")
    private Integer level;

    @Schema(description = "트리 경로", example = "/HQ001/L1-001")
    private String treePath;

    @Schema(description = "상위 협력사 ID (있는 경우)", example = "parent-uuid-123")
    private String parentPartnerId;

    // ====================================================================
    // CompanyProfile에서 가져오는 회사 정보
    // ====================================================================

    @Schema(description = "회사명 (정식 명칭)", example = "한국전력공사")
    private String corpName;

    @Schema(description = "영문 회사명", example = "Korea Electric Power Corporation")
    private String corpNameEng;

    @Schema(description = "주식 코드 (종목 코드)", example = "015760")
    private String stockCode;

    @Schema(description = "종목명", example = "한전")
    private String stockName;

    @Schema(description = "대표이사명", example = "김동철")
    private String ceoName;

    @Schema(description = "법인 구분 (Y: 유가증권시장, K: 코스닥, N: 코넥스, E: 기타)", example = "Y")
    private String corpClass;

    @Schema(description = "사업자등록번호", example = "123-45-67890")
    private String businessNumber;

    @Schema(description = "법인등록번호", example = "123456-7890123")
    private String corporateRegistrationNumber;

    @Schema(description = "주소", example = "전라남도 나주시 전력로 55")
    private String address;

    @Schema(description = "홈페이지 URL", example = "http://www.kepco.co.kr")
    private String homepageUrl;

    @Schema(description = "IR URL", example = "http://ir.kepco.co.kr")
    private String irUrl;

    @Schema(description = "전화번호", example = "02-3456-7890")
    private String phoneNumber;

    @Schema(description = "팩스번호", example = "02-3456-7891")
    private String faxNumber;

    @Schema(description = "업종명", example = "전기업")
    private String industry;

    @Schema(description = "업종 코드", example = "264")
    private String industryCode;

    @Schema(description = "설립일 (YYYYMMDD)", example = "19610715")
    private String establishmentDate;

    @Schema(description = "결산월 (MM)", example = "12")
    private String accountingMonth;

    @Schema(description = "CompanyProfile 데이터 최종 수정 일시", example = "2023-01-01T10:00:00")
    private LocalDateTime companyProfileUpdatedAt;
}