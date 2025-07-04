package com.nsmm.esg.dart_service.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 협력사 재무제표 이용 가능 기간 정보 DTO
 * 
 * 특정 협력사의 DB에 저장된 재무제표 데이터가 존재하는 연도/분기 조합을 나타냅니다.
 * 
 * @author ESG Project Team
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "협력사 재무제표 이용 가능 기간 정보")
public class AvailablePeriodDto {

  @Schema(description = "사업연도 (YYYY 형식)", example = "2023", required = true)
  private String bsnsYear;

  @Schema(description = "보고서 코드 (11011: 사업보고서, 11012: 반기보고서, 11013: 1분기보고서, 11014: 3분기보고서)", example = "11011", required = true)
  private String reprtCode;

  @Schema(description = "보고서명", example = "사업보고서", required = true)
  private String reprtName;

  @Schema(description = "기간 설명", example = "2023년 연간", required = true)
  private String periodDescription;

  @Schema(description = "해당 기간의 재무제표 항목 수", example = "131", required = true)
  private long itemCount;

  @Schema(description = "자동 선택 여부 (현재 날짜 기준으로 가장 최신인지)", example = "true")
  private boolean isAutoSelected;

  /**
   * 보고서 코드를 보고서명으로 변환
   */
  public static String getReportName(String reprtCode) {
    switch (reprtCode) {
      case "11011":
        return "사업보고서";
      case "11012":
        return "반기보고서";
      case "11013":
        return "1분기보고서";
      case "11014":
        return "3분기보고서";
      default:
        return "알 수 없는 보고서";
    }
  }

  /**
   * 보고서 코드를 기간 설명으로 변환
   */
  public static String getPeriodDescription(String bsnsYear, String reprtCode) {
    String reportName = getReportName(reprtCode);
    switch (reprtCode) {
      case "11011":
        return bsnsYear + "년 연간";
      case "11012":
        return bsnsYear + "년 상반기";
      case "11013":
        return bsnsYear + "년 1분기";
      case "11014":
        return bsnsYear + "년 3분기";
      default:
        return bsnsYear + "년 " + reportName;
    }
  }
}