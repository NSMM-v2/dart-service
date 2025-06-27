/**
 * @file DartCorpCodeXmlDto.java
 * @description DART API의 corpCode.xml 파일 내용을 매핑하기 위한 DTO 클래스입니다.
 */
package com.nsmm.esg.dart_service.dart.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
@JacksonXmlRootElement(localName = "result")
@Schema(description = "DART API의 corpCode.xml 파일(result 요소 내부에 status/message가 있는 기본 형식)을 매핑하기 위한 DTO")
public class DartCorpCodeXmlDto {

    @JacksonXmlProperty(localName = "status")
    @Schema(description = "API 응답 상태 코드", example = "000")
    private String status;

    @JacksonXmlProperty(localName = "message")
    @Schema(description = "API 응답 메시지", example = "정상")
    private String message;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "list")
    @Schema(description = "기업 코드 항목 리스트")
    private List<CorpCodeItem> list;

    @Data
    @Schema(description = "DART 기업 코드 XML 항목 (기본 형식)")
    public static class CorpCodeItem {
        @Schema(description = "DART 고유번호 (8자리)", example = "00126380")
        private String corpCode;
        @Schema(description = "정식 회사명", example = "삼성전자")
        private String corpName;
        @Schema(description = "영문 회사명", example = "SAMSUNG ELECTRONICS CO,.LTD", nullable = true)
        private String corpEngName;
        @Schema(description = "종목 코드 (상장된 경우, 6자리)", example = "005930", nullable = true)
        private String stockCode;
        @Schema(description = "정보 최종 수정일 (YYYYMMDD 형식)", example = "20230101")
        private String modifyDate;

        @JsonCreator
        public CorpCodeItem(
                @JacksonXmlProperty(localName = "corp_code") String corpCode,
                @JacksonXmlProperty(localName = "corp_name") String corpName,
                @JacksonXmlProperty(localName = "corp_eng_name") String corpEngName,
                @JacksonXmlProperty(localName = "stock_code") String stockCode,
                @JacksonXmlProperty(localName = "modify_date") String modifyDate) {
            this.corpCode = corpCode;
            this.corpName = corpName;
            this.corpEngName = corpEngName;
            this.stockCode = stockCode;
            this.modifyDate = modifyDate;
        }
    }
} 