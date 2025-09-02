/**
 * @file ResponseLoggingAdvice.java
 * @description 컨트롤러의 응답 본문(response body)을 클라이언트에 보내기 전에 이를 로깅하는 어드바이스 클래스입니다.
 * ResponseBodyAdvice 인터페이스를 구현하여 응답 처리 파이프라인에 통합되며, 상세한 응답 정보를 기록합니다.
 */
package com.nsmm.esg.dart_service.common.logging;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import com.nsmm.esg.dart_service.common.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.server.ServletServerHttpResponse;

@ControllerAdvice
@Slf4j
public class ResponseLoggingAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {

        // 응답 헤더 로깅
        log.info("=== 상세 응답 로깅 ===");
        log.info("응답 URI: {}", request.getURI());
        log.info("응답 헤더:");
        response.getHeaders().forEach((name, values) ->
                log.info("  {} : {}", name, values)
        );

        // 응답 본문 로깅
        try {
            if (body != null) {
                boolean isErrorResponse = body instanceof ErrorResponse;

                HttpStatus status = null;
                int statusCode = 0; // 기본값 또는 오류 시 사용할 값

                if (response instanceof HttpServletResponse httpServletResponse) {
                    statusCode = httpServletResponse.getStatus();
                } else if (response instanceof ServletServerHttpResponse servletServerHttpResponse) {
                    statusCode = servletServerHttpResponse.getServletResponse().getStatus();
                } else {
                    // HttpServletResponse 또는 ServletServerHttpResponse가 아닌 경우
                    // 안전하게 상태 코드를 가져올 표준적인 방법이 제한적입니다.
                    // Spring 6부터 ServerHttpResponse에 getStatusCode()가 추가되었으나,
                    // 하위 호환성 및 다양한 구현체를 고려하여 여기서는 특정 타입에 의존하지 않습니다.
                    log.warn("응답 객체 타입({})에서 직접 상태 코드를 가져오기 어렵습니다.", response.getClass().getName());
                    // 상태 코드를 알 수 없으므로, 오류 여부를 판단하기 어렵습니다.
                    // 이 경우, body가 ErrorResponse인지 여부로만 오류를 판단합니다.
                }

                if (statusCode != 0) { // statusCode가 0이 아닌 유효한 값일 때만 HttpStatus로 변환
                    try {
                        status = HttpStatus.valueOf(statusCode);
                    } catch (IllegalArgumentException e) {
                        log.warn("응답에서 유효한 HTTP 상태 코드를 변환할 수 없습니다: {}", statusCode, e);
                    }
                }

                // 오류 응답 로깅 강화: statusCode가 0인 경우(알 수 없는 경우)에도 isErrorResponse를 통해 오류 로깅 시도
                if (isErrorResponse || (status != null && status.isError())) {
                    log.error("오류 응답 발생: URI={}, 상태코드={}, 응답 본문={}", 
                              request.getURI(), 
                              statusCode != 0 ? String.valueOf(statusCode) : "알수없음", 
                              body);
                } else {
                    String bodyString = body.toString();
                    if (bodyString.length() > 1000) {
                        log.info("응답 본문: (길이: {} 자)", bodyString.length());
                    } else {
                        log.info("응답 본문: {}", body);
                    }
                }
            } else {
                log.info("응답 본문: null");
            }
        } catch (Exception e) {
            log.error("응답 로깅 중 오류 발생", e);
        }

        return body;
    }
}
