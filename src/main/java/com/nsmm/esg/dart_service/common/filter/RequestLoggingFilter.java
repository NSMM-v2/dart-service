/**
 * @file RequestLoggingFilter.java
 * @description HTTP 요청 및 응답에 대한 상세 정보를 로깅하는 필터입니다.
 *              요청 메서드, URI, 헤더, 파라미터, 본문 및 응답 상태 코드, 본문, 처리 시간 등을 기록하여 디버깅 및 모니터링을 용이하게 합니다.
 */
package com.nsmm.esg.dart_service.common.filter;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 모든 HTTP 요청과 응답을 로깅하는 필터
 * 요청 헤더, 파라미터, 바디와 응답 상태 코드, 바디를 로깅합니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 요청과 응답을 캐싱하여 여러 번 읽을 수 있도록 함
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // 요청 처리 전 로깅
            logRequestStart(requestWrapper);

            // 다음 필터 체인 실행
            filterChain.doFilter(requestWrapper, responseWrapper);

            // 요청 처리 후 로깅
            long duration = System.currentTimeMillis() - startTime;
            logRequestEnd(requestWrapper, responseWrapper, duration);

        } finally {
            // 응답 본문 복원 (필수)
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequestStart(ContentCachingRequestWrapper request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        log.info("=== 요청 시작: {} {} ===", method, uri);

        // 요청 헤더 로깅
        Map<String, String> headers = getRequestHeaders(request);
        log.info("요청 헤더: {}", headers);

        // 요청 파라미터 로깅
        Map<String, String[]> parameters = request.getParameterMap();
        if (!parameters.isEmpty()) {
            log.info("요청 파라미터: {}", parameters);
        }
    }

    private void logRequestEnd(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();

        // 응답 상태 및 본문 로깅
        String responseBody = getResponseBody(response);
        if (status >= 400) {
            log.error("=== 요청 종료: {} {} - {} ({} ms) ===", method, uri, status, duration);
            log.error("응답 본문: {}", responseBody);
        } else {
            log.info("=== 요청 종료: {} {} - {} ({} ms) ===", method, uri, status, duration);
            if (responseBody.length() < 1000) {  // 응답이 너무 길지 않은 경우만 로깅
                log.info("응답 본문: {}", responseBody);
            } else {
                log.info("응답 본문: (길이: {} 자)", responseBody.length());
            }
        }
    }

    private Map<String, String> getRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        return headers;
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        try {
            return new String(content, request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            log.error("요청 본문 인코딩 오류", e);
            return "인코딩 오류로 본문을 읽을 수 없습니다";
        }
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        try {
            return new String(content, response.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            log.error("응답 본문 인코딩 오류", e);
            return "인코딩 오류로 본문을 읽을 수 없습니다";
        }
    }
}
