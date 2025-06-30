/**
 * @file CompanyProfileResponse.java
 * @description DART API에서 회사 정보를 조회한 결과를 매핑하기 위한 DTO 클래스입니다.
 */
package com.nsmm.esg.dart_service.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DART API로부터 받은 회사 개황 정보 응답 DTO")
public class CompanyProfileResponse {

    @JsonProperty("status")
    @Schema(description = "API 응답 상태 코드 (성공: 000, 그 외 오류 코드)", example = "000")
    private String status;

    @JsonProperty("message")
    @Schema(description = "API 응답 메시지", example = "정상")
    private String message;

    @JsonProperty("corp_code")
    @Schema(description = "DART 고유번호 (8자리)", example = "00126380")
    private String corpCode;

    @JsonProperty("corp_name")
    @Schema(description = "정식 회사명", example = "삼성전자")
    private String corpName;

    @JsonProperty("corp_name_eng")
    @Schema(description = "영문 회사명", example = "SAMSUNG ELECTRONICS CO,.LTD")
    private String corpNameEng;

    @JsonProperty("stock_name")
    @Schema(description = "종목명", example = "삼성전자")
    private String stockName;

    @JsonProperty("stock_code")
    @Schema(description = "종목 코드 (상장된 경우, 6자리)", example = "005930", nullable = true)
    private String stockCode;

    @JsonProperty("ceo_nm")
    @Schema(description = "대표이사명", example = "한종희")
    private String ceoName;

    @JsonProperty("corp_cls")
    @Schema(description = "법인 구분 (Y: 유가증권시장, K: 코스닥시장, N: 코넥스시장, E: 기타)", example = "Y")
    private String corpClass;

    @JsonProperty("jurir_no")
    @Schema(description = "법인등록번호", example = "1301110006246")
    private String corporateRegistrationNumber;

    @JsonProperty("bizr_no")
    @Schema(description = "사업자등록번호", example = "1248100998")
    private String businessNumber;

    @JsonProperty("adres")
    @Schema(description = "주소", example = "경기도 수원시 영통구 삼성로 129 (매탄동)")
    private String address;

    @JsonProperty("hm_url")
    @Schema(description = "홈페이지 URL", example = "www.samsung.com/sec", nullable = true)
    private String homepageUrl;

    @JsonProperty("ir_url")
    @Schema(description = "IR 홈페이지 URL", example = "", nullable = true)
    private String irUrl;

    @JsonProperty("phn_no")
    @Schema(description = "전화번호", example = "02-2255-0114", nullable = true)
    private String phoneNumber;

    @JsonProperty("fax_no")
    @Schema(description = "팩스번호", example = "031-200-7538", nullable = true)
    private String faxNumber;

    @JsonProperty("induty_code")
    @Schema(description = "업종 코드", example = "264")
    private String industryCode;

    @JsonProperty("est_dt")
    @Schema(description = "설립일 (YYYYMMDD)", example = "19690113")
    private String establishmentDate;

    @JsonProperty("acc_mt")
    @Schema(description = "결산월 (MM)", example = "12")
    private String accountingMonth;
}