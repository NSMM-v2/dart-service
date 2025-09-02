/**
 * @file CorpCodeQueryDto.java
 * @description 기업 코드 조회를 위한 요청 DTO입니다.
 */
package com.nsmm.esg.dart_service.dart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DART 기업 코드 목록 조회 시 사용되는 쿼리 파라미터 DTO")
public class CorpCodeQueryDto {

    @Schema(description = "조회할 페이지 번호 (1부터 시작)", defaultValue = "1", example = "1")
    private int page = 1;

    @Schema(description = "페이지당 표시할 항목 수", defaultValue = "10", example = "10")
    private int pageSize = 10;

    @Schema(description = "상장된 기업만 조회할지 여부 (true: 상장 기업만, false: 전체 기업, 기본값: false)", defaultValue = "false", example = "true")
    private boolean listedOnly = false;

    @Schema(description = "검색할 회사명 (부분 일치, 대소문자 구분 없음)", example = "삼성", nullable = true)
    private String corpNameFilter;
} 