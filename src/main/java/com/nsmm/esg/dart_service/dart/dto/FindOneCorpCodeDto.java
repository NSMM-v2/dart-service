/**
 * @file FindOneCorpCodeDto.java
 * @description 단일 기업 코드 조회를 위한 요청 DTO입니다.
 */
package com.nsmm.esg.dart_service.dart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내부 서비스 로직에서 단일 기업 코드를 특정하기 위한 파라미터를 담는 DTO")
public class FindOneCorpCodeDto {

    @Schema(description = "DART에서 발급하는 고유한 회사 코드 (8자리)", example = "00126380", nullable = true)
    private String corpCode;

    @Schema(description = "주식 시장에서 사용하는 종목 코드 (6자리, 상장된 경우)", example = "005930", nullable = true)
    private String stockCode;

    @Schema(description = "회사명 (검색용)", example = "삼성전자", nullable = true)
    private String corpName;
} 