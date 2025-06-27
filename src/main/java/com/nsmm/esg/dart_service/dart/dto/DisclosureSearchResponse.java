/**
 * @file DisclosureSearchResponse.java
 * @description DART API에서 공시 정보 검색 결과를 매핑하기 위한 DTO 클래스입니다.
 */
package com.nsmm.esg.dart_service.dart.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DART API 공시 정보 검색 결과 응답 DTO")
public class DisclosureSearchResponse {
    
    @JsonProperty("status")
    @Schema(description = "API 응답 상태 코드 (성공: 000, 그 외 오류 코드)", example = "000")
    private String status;
    
    @JsonProperty("message")
    @Schema(description = "API 응답 메시지", example = "정상")
    private String message;
    
    @JsonProperty("page_no")
    @Schema(description = "페이지 번호", example = "1")
    private int pageNo;
    
    @JsonProperty("page_count")
    @Schema(description = "페이지당 표시 건수", example = "10")
    private int pageCount;
    
    @JsonProperty("total_count")
    @Schema(description = "총 건수", example = "150")
    private int totalCount;
    
    @JsonProperty("total_page")
    @Schema(description = "총 페이지 수", example = "15")
    private int totalPage;
    
    @JsonProperty("list")
    @Schema(description = "공시 정보 항목 리스트")
    private List<DisclosureItem> list;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "개별 공시 정보 항목")
    public static class DisclosureItem {
        
        @JsonProperty("corp_code")
        @Schema(description = "DART 고유번호 (8자리)", example = "00126380")
        private String corpCode;
        
        @JsonProperty("corp_name")
        @Schema(description = "회사명", example = "삼성전자")
        private String corpName;
        
        @JsonProperty("stock_code")
        @Schema(description = "종목 코드 (상장된 경우, 6자리)", example = "005930", nullable = true)
        private String stockCode;
        
        @JsonProperty("corp_cls")
        @Schema(description = "법인 구분 (Y: 유가, K: 코스닥, N: 코넥스, E: 기타)", example = "Y")
        private String corpClass;
        
        @JsonProperty("report_nm")
        @Schema(description = "보고서명", example = "사업보고서 (2022.12)")
        private String reportName;
        
        @JsonProperty("rcept_no")
        @Schema(description = "접수번호 (공시 고유번호)", example = "20230315000123")
        private String receiptNo;
        
        @JsonProperty("flr_nm")
        @Schema(description = "제출인명", example = "삼성전자")
        private String submitterName;
        
        @JsonProperty("rcept_dt")
        @Schema(description = "접수일자 (YYYYMMDD 형식)", example = "20230315")
        private String receiptDate;
        
        @JsonProperty("rm")
        @Schema(description = "비고 (예: 유, 정정, 첨부정정 등)", example = "유", nullable = true)
        private String remark;
    }
} 