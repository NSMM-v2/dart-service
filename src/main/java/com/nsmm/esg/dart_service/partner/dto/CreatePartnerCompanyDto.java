/**
 * @file CreatePartnerCompanyDto.java
 * @description 새 협력사 등록 요청 시 사용되는 DTO입니다.
 *              DART 기업 고유 코드를 통해 CompanyProfile에서 회사 정보를 조회하여 협력사를 등록합니다.
 *              회사명, 주식 코드 등 상세 정보는 CompanyProfile에서 자동으로 가져옵니다.
 */
package com.nsmm.esg.dart_service.partner.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "신규 협력사 등록 요청 시 사용되는 DTO. 회사 정보는 CompanyProfile에서 자동 조회됩니다.")
public class CreatePartnerCompanyDto {

    @NotBlank(message = "DART 기업 고유 코드는 필수 입력 항목입니다.")
    @Size(min = 8, max = 8, message = "DART 기업 고유 코드는 8자리여야 합니다.")
    @Schema(description = "DART 기업 고유 코드 (8자리 숫자). 이 코드를 기준으로 CompanyProfile에서 회사 정보를 조회합니다.", example = "00123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String corpCode;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "계약 시작일은 필수 입력 항목입니다.")
    @Schema(description = "협력사와의 계약 시작일 (YYYY-MM-DD 형식)", example = "2023-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate contractStartDate;

    // ====================================================================
    // 편의 메서드
    // ====================================================================

    /**
     * 로그 출력용 회사명 반환
     * 실제 회사명은 CompanyProfile에서 조회됩니다.
     * 
     * @return 로그용 임시 표시명
     */
    public String getCompanyName() {
        return "DART:" + corpCode; // 로그용 임시 표시명
    }
}