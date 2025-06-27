/**
 * @file CreatePartnerCompanyDto.java
 * @description 새 파트너사 등록 요청 시 사용되는 DTO입니다.
 *              필수 정보인 회사명, DART 기업 고유 코드, 계약 시작일을 포함합니다.
 *              기존 필드 중 contractEndDate, industry, country, address는 사용자 요청에 의해 제거되었습니다.
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
@Schema(description = "신규 파트너사 등록 요청 시 사용되는 DTO")
public class CreatePartnerCompanyDto {
    
    @NotBlank(message = "회사명은 필수 입력 항목입니다.")
    @Size(max = 255, message = "회사명은 최대 255자까지 입력 가능합니다.")
    @Schema(description = "파트너 회사명", example = "주식회사 새협력", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;
    
    @NotBlank(message = "DART 기업 고유 코드는 필수 입력 항목입니다.")
    @Size(min = 8, max = 8, message = "DART 기업 고유 코드는 8자리여야 합니다.")
    @Schema(description = "DART 기업 고유 코드 (8자리 숫자). 이 코드를 기준으로 DART에서 추가 정보를 조회합니다.", example = "00123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String corpCode;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "계약 시작일은 필수 입력 항목입니다.")
    @Schema(description = "파트너사와의 계약 시작일 (YYYY-MM-DD 형식)", example = "2023-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate contractStartDate;
    
    @Size(max = 10, message = "주식 코드는 최대 10자까지 입력 가능합니다.")
    @Schema(description = "주식 코드 (선택사항, DART API에서 자동으로 조회되지만 수동으로도 설정 가능)", example = "005930", nullable = true)
    private String stockCode;
}