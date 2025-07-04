/**
 * @file UpdatePartnerCompanyDto.java
 * @description 기존 협력사 정보 수정 요청 시 사용되는 DTO입니다.
 *              모든 필드는 선택적으로 제공될 수 있으며, 제공된 필드만 업데이트됩니다.
 *              회사 정보는 CompanyProfile과의 연관관계를 통해 관리되므로 corpCode 변경 시
 *              CompanyProfile에서 자동으로 회사 정보를 조회합니다.
 */
package com.nsmm.esg.dart_service.partner.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.nsmm.esg.dart_service.partner.model.PartnerCompanyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "협력사 정보 업데이트 요청 시 사용되는 DTO. 모든 필드는 선택 사항입니다.")
public class UpdatePartnerCompanyDto {

    @Size(min = 8, max = 8, message = "DART 기업 고유 코드는 8자리여야 합니다.")
    @Schema(description = "변경할 DART 기업 고유 코드 (8자리 숫자). 변경 시 CompanyProfile에서 회사 정보를 다시 조회합니다.", example = "00654321", nullable = true)
    private String corpCode;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "변경할 협력사와의 계약 시작일 (YYYY-MM-DD 형식)", example = "2024-01-01", nullable = true)
    private LocalDate contractStartDate;

    @Schema(description = "변경할 협력사 상태 (예: ACTIVE, INACTIVE). 상태 변경은 신중해야 합니다.", example = "ACTIVE", nullable = true)
    private PartnerCompanyStatus status;

    // ====================================================================
    // 참고사항
    // ====================================================================
    // 1. 회사명, 주소, 업종 등의 회사 정보는 CompanyProfile에서 관리되므로
    // 이 DTO에서 직접 수정할 수 없습니다.
    // 2. corpCode를 변경하면 새로운 CompanyProfile과 연결되어
    // 회사 정보가 자동으로 업데이트됩니다.
    // 3. 소유자 정보(headquartersId, partnerId)는 등록 시 결정되며
    // 이후 변경할 수 없습니다.
    // ====================================================================
}