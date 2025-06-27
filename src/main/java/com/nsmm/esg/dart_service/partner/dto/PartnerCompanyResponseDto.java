/**
 * @file PartnerCompanyResponseDto.java
 * @description 파트너사 정보 조회 및 등록/수정 결과 응답 시 사용되는 DTO입니다.
 *              파트너사의 주요 정보를 포함하며, `PartnerCompany` 엔티티의 상세 정보를 반영합니다.
 *              필요에 따라 특정 필드는 nullable로 처리될 수 있습니다.
 */
package com.nsmm.esg.dart_service.partner.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.nsmm.esg.dart_service.partner.model.PartnerCompanyStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "파트너사 정보 응답 DTO. 파트너사의 상세 정보를 나타냅니다.")
public class PartnerCompanyResponseDto {
    
    @Schema(description = "파트너사 레코드의 고유 ID (UUID 형식)", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;


    @JsonProperty("corp_code")
    @Schema(description = "DART 기업 고유번호 (8자리). 파트너사가 DART에 등록된 경우 제공됩니다.", example = "00123456", nullable = true)
    private String corpCode;
    
    @JsonProperty("corp_name")
    @Schema(description = "파트너 회사명", example = "주식회사 협력업체", requiredMode = Schema.RequiredMode.REQUIRED)
    private String corpName;
    
    @JsonProperty("stock_code")
    @Schema(description = "주식 종목 코드 (6자리). 파트너사가 상장사이고 정보가 있는 경우 제공됩니다.", example = "005930", nullable = true)
    private String stockCode;
    
    @JsonProperty("contract_start_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "파트너사와의 계약 시작일 (YYYY-MM-DD 형식)", example = "2023-01-01", nullable = true)
    private LocalDate contractStartDate;
    
    @JsonProperty("modify_date")
    @Schema(description = "파트너사 정보 최종 수정일 (YYYYMMDD 형식). 이 필드는 엔티티의 updatedAt 값을 기반으로 생성됩니다.", example = "20240516")
    private String modifyDate;
    
    @Schema(description = "파트너사 상태 (예: ACTIVE - 활동, INACTIVE - 비활동)", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    private PartnerCompanyStatus status;
    
    @Schema(description = "파트너사의 업종 정보", example = "소프트웨어 개발 및 공급업", nullable = true)
    private String industry;
    
    @Schema(description = "파트너사의 국가 정보", example = "대한민국", nullable = true)
    private String country;
    
    @Schema(description = "파트너사의 주소 정보", example = "서울특별시 강남구 테헤란로 123, 45층", nullable = true)
    private String address;
    
    @JsonProperty("is_restored")
    @Schema(description = "파트너사가 복원되었는지 여부 (true: 비활성 상태에서 복원됨, false: 새로 생성됨)", example = "false", nullable = true)
    private Boolean isRestored;
    
    /**
     * LocalDateTime을 YYYYMMDD 형식의 문자열로 변환합니다.
     * 
     * @param dateTime 변환할 LocalDateTime
     * @return YYYYMMDD 형식의 문자열
     */
    public static String formatDateToYYYYMMDD(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}