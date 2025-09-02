/**
 * @file ErrorResponse.java
 * @description 오류 응답을 위한 표준화된 형식을 제공합니다.
 */
package com.nsmm.esg.dart_service.common.exception;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API 오류 발생 시 반환되는 표준 응답 형식")
public class ErrorResponse {
    
    @Schema(description = "HTTP 상태 코드", example = "404")
    private int status;
    
    @Schema(description = "오류 메시지 요약", example = "요청한 리소스를 찾을 수 없습니다.")
    private String message;
    
    @Schema(description = "오류 상세 내용 (예: 요청 경로)", example = "uri=/partners/partner-companies/non-existent-id")
    private String details;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "오류 발생 시간", example = "2024-05-17 10:30:00")
    private LocalDateTime timestamp;
} 