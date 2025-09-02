/**
 * @file FinancialStatementResponseDto.java
 * @description DART API의 단일회사 전체 재무제표 조회 결과를 매핑하기 위한 DTO 클래스입니다.
 */
package com.nsmm.esg.dart_service.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DART API 단일회사 전체 재무제표 조회 결과 응답 DTO")
public class FinancialStatementResponseDto {

    @JsonProperty("status")
    @Schema(description = "API 응답 상태 코드 (성공: 000, 그 외 오류 코드)", example = "000")
    private String status;

    @JsonProperty("message")
    @Schema(description = "API 응답 메시지", example = "정상")
    private String message;

    @JsonProperty("list")
    @Schema(description = "재무제표 항목 리스트")
    private List<FinancialStatementItem> list;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "개별 재무제표 항목")
    public static class FinancialStatementItem {

        @JsonProperty("rcept_no")
        @Schema(description = "접수번호 (공시 고유번호)", example = "20230315000123")
        private String rceptNo; // 접수번호

        @JsonProperty("reprt_code")
        @Schema(description = "보고서 코드 (11011: 사업보고서, 11012: 반기보고서, 11013: 1분기보고서, 11014: 3분기보고서)", example = "11011")
        private String reprtCode; // 보고서 코드 (11011: 사업보고서, 11012: 반기보고서, 11013: 1분기보고서, 11014: 3분기보고서)

        @JsonProperty("bsns_year")
        @Schema(description = "사업 연도 (YYYY 형식)", example = "2022")
        private String bsnsYear; // 사업 연도

        @JsonProperty("corp_code")
        @Schema(description = "DART 고유번호 (8자리)", example = "00126380")
        private String corpCode; // 고유번호

        @JsonProperty("sj_div")
        @Schema(description = "재무제표구분 (BS: 재무상태표, IS: 손익계산서, CIS: 포괄손익계산서, CF: 현금흐름표, SCE: 자본변동표)", example = "BS")
        private String sjDiv; // 재무제표구분 (BS: 재무상태표, IS: 손익계산서, CIS: 포괄손익계산서, CF: 현금흐름표, SCE: 자본변동표)

        @JsonProperty("sj_nm")
        @Schema(description = "재무제표명", example = "재무상태표")
        private String sjNm; // 재무제표명

        @JsonProperty("account_id")
        @Schema(description = "계정ID (XBRL 표준계정ID 또는 DART 자체 계정 ID)", example = "ifrs-full_CurrentAssets")
        private String accountId; // 계정ID (XBRL 표준계정ID)

        @JsonProperty("account_nm")
        @Schema(description = "계정명", example = "유동자산")
        private String accountNm; // 계정명 (예: 유동자산, 매출액, 영업이익, 당기순이익)

        @JsonProperty("account_detail")
        @Schema(description = "계정상세 (자본변동표에 주로 사용됨)", example = "-", nullable = true)
        private String accountDetail; // 계정상세 (자본변동표에만 출력)

        @JsonProperty("thstrm_nm")
        @Schema(description = "당기명 (예: 제 54 기)", example = "제 54 기")
        private String thstrmNm; // 당기명 (예: 제 13 기)

        @JsonProperty("thstrm_amount")
        @Schema(description = "당기금액 (숫자형 문자열, 단위: 원)", example = "1890123456789")
        private String thstrmAmount; // 당기금액 (분/반기 보고서 손익계산서의 경우 3개월 금액)

        @JsonProperty("thstrm_add_amount")
        @Schema(description = "당기누적금액 (손익계산서의 경우 사용, 숫자형 문자열, 단위: 원)", example = "1890123456789", nullable = true)
        private String thstrmAddAmount; // 당기누적금액 (손익계산서)

        @JsonProperty("frmtrm_nm")
        @Schema(description = "전기명", example = "제 53 기")
        private String frmtrmNm; // 전기명

        @JsonProperty("frmtrm_amount")
        @Schema(description = "전기금액", example = "1780123456789")
        private String frmtrmAmount; // 전기금액

        @JsonProperty("frmtrm_q_nm")
        @Schema(description = "전기분기명 (분/반기 보고서의 경우 사용)", example = "제 53 기 3분기말", nullable = true)
        private String frmtrmQNm; // 전기명(분/반기)

        @JsonProperty("frmtrm_q_amount")
        @Schema(description = "전기분기금액 (분/반기 보고서의 경우 사용)", example = "1770123456789", nullable = true)
        private String frmtrmQAmount; // 전기금액(분/반기)

        @JsonProperty("frmtrm_add_amount")
        @Schema(description = "전기누적금액 (손익계산서의 경우 사용)", example = "1780123456789", nullable = true)
        private String frmtrmAddAmount; // 전기누적금액

        @JsonProperty("bfefrmtrm_nm")
        @Schema(description = "전전기명", example = "제 52 기", nullable = true)
        private String bfefrmtrmNm; // 전전기명

        @JsonProperty("bfefrmtrm_amount")
        @Schema(description = "전전기금액", example = "1670123456789", nullable = true)
        private String bfefrmtrmAmount; // 전전기금액
        
        @JsonProperty("ord")
        @Schema(description = "계정과목 정렬순서", example = "1")
        private String ord; // 계정과목 정렬순서

        @JsonProperty("currency")
        @Schema(description = "통화 단위", example = "KRW")
        private String currency; // 통화 단위
    }
} 