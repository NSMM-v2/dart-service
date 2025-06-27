/**
 * @file RequestLoggingAdvice.java
 * @description 컨트롤러의 요청 본문(request body)을 읽은 후 이를 로깅하는 어드바이스 클래스입니다.
 *              RequestBodyAdviceAdapter를 확장하여 요청 처리 파이프라인에 통합되며, 상세한 요청 정보를 기록합니다.
 */
package com.nsmm.esg.dart_service.common.logging;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Enumeration;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ControllerAdvice
@Slf4j
public class RequestLoggingAdvice extends RequestBodyAdviceAdapter {

    @Override
    public boolean supports(
            @NonNull MethodParameter methodParameter,
            @NonNull Type targetType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    @Override
    @NonNull
    public Object afterBodyRead(
            @NonNull Object body,
            @NonNull HttpInputMessage inputMessage,
            @NonNull MethodParameter parameter,
            @NonNull Type targetType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {

        logRequest(body);
        return body;
    }

    private void logRequest(Object body) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                var request = attributes.getRequest();

                // 요청 기본 정보 로깅
                log.info("=== 상세 요청 로깅 ===");
                log.info("요청 URI: {}", request.getRequestURI());
                log.info("요청 URL: {}", request.getRequestURL());
                log.info("요청 메소드: {}", request.getMethod());
                log.info("요청 IP: {}", request.getRemoteAddr());

                // 요청 헤더 로깅
                Enumeration<String> headerNames = request.getHeaderNames();
                if (headerNames != null) {
                    log.info("요청 헤더:");
                    while (headerNames.hasMoreElements()) {
                        String headerName = headerNames.nextElement();
                        log.info("  {} : {}", headerName, request.getHeader(headerName));
                    }
                }

                // 요청 파라미터 로깅
                log.info("요청 파라미터:");
                request.getParameterMap().forEach((key, value) -> log.info("  {} : {}", key, Arrays.toString(value)));

                // 요청 본문 로깅 (있는 경우)
                if (body != null) {
                    log.info("요청 본문: {}", body);
                }
            }
        } catch (Exception e) {
            log.error("요청 로깅 중 오류 발생", e);
        }
    }
}