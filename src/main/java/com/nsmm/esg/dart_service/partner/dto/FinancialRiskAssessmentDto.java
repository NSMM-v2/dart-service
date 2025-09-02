/**
 * @file FinancialRiskAssessmentDto.java
 * @description 파트너사의 재무 위험 분석 결과를 담는 DTO입니다.
 *              파트너사 정보, 분석 기준 연도 및 보고서 코드, 그리고 각 위험 항목별 분석 결과를 포함합니다.
 */
package com.nsmm.esg.dart_service.partner.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@Schema(description = "파트너사의 재무 위험 분석 결과를 담는 DTO")
public class FinancialRiskAssessmentDto {

    @Schema(description = "협력사의 DART 고유번호 (8자리)", example = "00126380")
    private String partnerCompanyId;
    @Schema(description = "협력 회사명", example = "주식회사 테스트협력")
    private String partnerCompanyName;
    @Schema(description = "분석 기준 사업연도 (YYYY 형식)", example = "2023")
    private String assessmentYear;
    @Schema(description = "분석 기준 보고서 코드 (예: 11011-사업보고서)", example = "11011")
    private String reportCode;

    @Schema(description = "각 재무 위험 항목별 분석 결과 (번호순으로 정렬된 배열)")
    private List<NumberedRiskItemResult> riskItems;

    @Data
    @Builder
    @Schema(description = "개별 재무 위험 항목 분석 결과")
    public static class RiskItemResult {
        @Schema(description = "해당 위험 항목에 해당하는지 여부 (true: 위험, false: 양호)", example = "true")
        private boolean isAtRisk;
        @Schema(description = "위험 항목에 대한 설명", example = "매출액 30% 이상 감소")
        private String description; 
        @Schema(description = "분석된 실제 값 (예: 증감률, 회전율 등)", example = "-35.20%")
        private String actualValue; 
        @Schema(description = "위험 판단 기준값 또는 임계치", example = "<= -30%")
        private String threshold;   
        @Schema(description = "추가적인 참고사항 또는 계산 근거", example = "전기 매출액: 100억, 당기 매출액: 64.8억", nullable = true)
        private String notes;       
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Schema(description = "번호가 부여된 재무 위험 항목 분석 결과")
    public static class NumberedRiskItemResult extends RiskItemResult {
        @Schema(description = "위험 항목 번호", example = "1")
        private int itemNumber;

        @Builder(builderMethodName = "numberedBuilder")
        public NumberedRiskItemResult(boolean isAtRisk, String description, String actualValue, 
                                     String threshold, String notes, int itemNumber) {
            super(isAtRisk, description, actualValue, threshold, notes);
            this.itemNumber = itemNumber;
        }
    }
} 
