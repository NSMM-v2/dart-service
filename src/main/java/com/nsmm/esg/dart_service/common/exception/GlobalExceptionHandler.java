/**
 * @file GlobalExceptionHandler.java
 * @description 애플리케이션 전역 예외 처리기입니다.
 *              다양한 예외를 처리하고 적절한 HTTP 상태 코드와 응답 메시지를 반환합니다.
 */
package com.nsmm.esg.dart_service.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

        private final ObjectMapper objectMapper;

        @ExceptionHandler(ResourceNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        @ApiResponse(responseCode = "404", description = "요청한 리소스를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
                        ResourceNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false),
                                LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        @ApiResponse(responseCode = "400", description = "요청 데이터 유효성 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<ErrorResponse> handleValidationExceptions(
                        MethodArgumentNotValidException ex, WebRequest request) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                log.error("유효성 검증 실패: {}, 요청 정보: {}", errors, request.getDescription(true));
                log.error("유효성 검증 실패 상세 - 바인딩 결과: {}", ex.getBindingResult());

                String details;
                try {
                        details = objectMapper.writeValueAsString(errors);
                } catch (JsonProcessingException e) {
                        log.error("오류 메시지 JSON 직렬화 실패", e);
                        details = errors.toString();
                }

                ErrorResponse errorResponse = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "유효성 검증 실패",
                                details,
                                LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(RestClientException.class)
        @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
        @ApiResponse(responseCode = "503", description = "외부 API 호출 실패 또는 서비스 사용 불가", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<ErrorResponse> handleRestClientException(
                        RestClientException ex, WebRequest request) {
                log.error("외부 API 호출 오류", ex);
                ErrorResponse errorResponse = new ErrorResponse(
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "외부 API 호출 중 오류가 발생했습니다",
                                ex.getMessage(),
                                LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }

        @ExceptionHandler(MissingServletRequestParameterException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        @ApiResponse(responseCode = "400", description = "필수 요청 파라미터 누락", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
                        MissingServletRequestParameterException ex, WebRequest request) {
                log.error("필수 요청 파라미터 누락: {}, 파라미터 타입: {}, 요청 정보: {}",
                                ex.getParameterName(), ex.getParameterType(), request.getDescription(true));

                ErrorResponse errorResponse = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "필수 요청 파라미터가 누락되었습니다",
                                "파라미터 '" + ex.getParameterName() + "'이(가) 필요합니다",
                                LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        @ApiResponse(responseCode = "400", description = "요청 본문(Body) 파싱 오류 또는 형식 잘못됨", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
                        HttpMessageNotReadableException ex, WebRequest request) {
                log.error("요청 본문을 읽을 수 없음: {}, 요청 정보: {}", ex.getMessage(), request.getDescription(true));

                ErrorResponse errorResponse = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "요청 본문을 읽을 수 없습니다",
                                "유효한 JSON 형식인지 확인하세요",
                                LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        @ApiResponse(responseCode = "400", description = "요청 파라미터 타입 불일치", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
                        MethodArgumentTypeMismatchException ex, WebRequest request) {
                log.error("메소드 인자 타입 불일치: 파라미터 '{}', 값 '{}', 필요한 타입: {}, 요청 정보: {}",
                                ex.getName(), ex.getValue(), ex.getRequiredType(), request.getDescription(true));

                ErrorResponse errorResponse = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "파라미터 타입이 올바르지 않습니다",
                                "파라미터 '" + ex.getName() + "'의 값 '" + ex.getValue() +
                                                "'은(는) " + ex.getRequiredType() + " 타입이어야 합니다",
                                LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(ResponseStatusException.class)
        @ApiResponse(responseCode = "409", description = "요청 충돌", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<ErrorResponse> handleResponseStatusException(
                        ResponseStatusException ex, WebRequest request) {
                log.warn("ResponseStatusException 발생: 상태코드={}, 메시지={}, 요청정보={}",
                                ex.getStatusCode(), ex.getReason(), request.getDescription(true));

                ErrorResponse errorResponse = new ErrorResponse(
                                ex.getStatusCode().value(),
                                ex.getReason() != null ? ex.getReason() : "요청 처리 중 오류가 발생했습니다",
                                ex.getMessage(),
                                LocalDateTime.now());
                return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
        }

        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        @ApiResponse(responseCode = "500", description = "처리되지 않은 서버 내부 오류 발생", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        public ResponseEntity<ErrorResponse> handleGlobalException(
                        Exception ex, WebRequest request) {
                log.error("처리되지 않은 예외 발생", ex);
                log.error("예외 클래스: {}, 메시지: {}, 요청 정보: {}",
                                ex.getClass().getName(), ex.getMessage(), request.getDescription(true));

                ErrorResponse errorResponse = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "서버 내부 오류가 발생했습니다",
                                ex.getMessage(),
                                LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
}
