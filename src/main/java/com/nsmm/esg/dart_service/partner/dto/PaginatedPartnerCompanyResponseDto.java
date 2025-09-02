/**
 * @file PaginatedPartnerCompanyResponseDto.java
 * @description 페이지네이션을 포함한 협력사 목록 응답 DTO입니다.
 */
package com.nsmm.esg.dart_service.partner.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이지네이션된 협력사 목록 응답 DTO")
public class PaginatedPartnerCompanyResponseDto {
    
    @Schema(description = "현재 페이지의 협력사 목록 데이터")
    private List<PartnerCompanyResponseDto> data;
    
    @Schema(description = "전체 협력사 개수 (필터링된 경우 필터링된 개수)", example = "123")
    private long total;
    
    @Schema(description = "현재 페이지 번호 (1부터 시작)", example = "1")
    private int page;
    
    @Schema(description = "페이지당 표시된 항목 수", example = "10")
    private int pageSize;
}