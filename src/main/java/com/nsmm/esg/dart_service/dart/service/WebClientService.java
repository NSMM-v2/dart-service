/**
 * @file WebClientService.java
 * @description WebClient를 사용한 API 호출을 담당하는 서비스 클래스입니다.
 *              DART API와의 통신을 처리합니다.
 */
package com.nsmm.esg.dart_service.dart.service;

import com.nsmm.esg.dart_service.dart.dto.CompanyProfileResponse;
import com.nsmm.esg.dart_service.dart.dto.DisclosureSearchResponse;
import com.nsmm.esg.dart_service.dart.dto.FinancialStatementResponseDto;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebClientService {

    @Value("${dart.api.key}")
    private String apiKey;

    @Value("${dart.api.base-url}")
    private String baseUrl;

    @Value("${dart.api.timeout:30}")
    private int timeout; // 기본 타임아웃 30초

    private final WebClient.Builder webClientBuilder;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        // API 키 마스킹 처리 (보안)
        String maskedApiKey = apiKey != null && apiKey.length() > 8
                ? apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4)
                : "null 또는 너무 짧음";

        log.info("WebClientService 초기화 - 타임아웃: {}초, API 키: {}, baseUrl: {}",
                timeout, maskedApiKey, baseUrl);

        this.webClient = webClientBuilder.baseUrl(baseUrl)
                .build();
    }

    /**
     * DART API에서 기업 코드 ZIP 파일을 다운로드합니다.
     *
     * @return ZIP 파일 데이터 버퍼 Flux
     */
    @RateLimiter(name = "dartApi")
    public Flux<DataBuffer> downloadCorpCodeZip() {
        log.info("DART API에서 기업 코드 ZIP 파일 다운로드 시작");

        String uri = "/api/corpCode.xml?crtfc_key=" + apiKey;
        log.debug("기업 코드 다운로드 API 요청 URI: {}", uri);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/corpCode.xml")
                        .queryParam("crtfc_key", apiKey)
                        .build())
                .exchangeToFlux(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToFlux(DataBuffer.class)
                                .doOnNext(dataBuffer -> {
                                    if (log.isDebugEnabled()) {
                                        try {
                                            // 데이터 버퍼를 복제하여 로깅에 사용 (원본 버퍼는 변경하지 않음)
                                            byte[] bytes = new byte[Math.min(dataBuffer.readableByteCount(), 100)];
                                            // 데이터 버퍼의 내용을 복사
                                            dataBuffer.asByteBuffer().get(bytes);
                                            log.debug("ZIP 데이터 샘플 (첫 100바이트): {}",
                                                    new String(bytes, StandardCharsets.UTF_8));
                                        } catch (Exception e) {
                                            log.warn("ZIP 데이터 샘플 로깅 실패", e);
                                        }
                                    }
                                });
                    } else if (response.statusCode().is4xxClientError()) {
                        log.error("기업 코드 다운로드 API 클라이언트 오류: {}", response.statusCode());
                        return response.bodyToMono(String.class)
                                .flatMapMany(errorBody -> {
                                    log.error("기업 코드 다운로드 API 오류 응답 본문: {}", errorBody);
                                    return Flux.error(new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST, "DART API 요청 오류: " + errorBody));
                                });
                    } else {
                        log.error("기업 코드 다운로드 API 서버 오류: {}", response.statusCode());
                        return Flux.error(new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "DART API 서버 오류가 발생했습니다."));
                    }
                })
                .timeout(Duration.ofSeconds(timeout))
                .doOnError(error -> log.error("기업 코드 다운로드 중 오류 발생: {}", error.getMessage(), error));
    }

    /**
     * DART API에서 회사 정보를 조회합니다.
     *
     * @param corpCode 회사 코드
     * @return 회사 정보 응답 Mono
     */
    @RateLimiter(name = "dartApi")
    public Mono<CompanyProfileResponse> getCompanyProfile(String corpCode) {
        log.info("[WebClientService] getCompanyProfile 메소드 시작: corpCode={}", corpCode);

        // API 키 상태 재확인
        log.info("[WebClientService] API 키 상태 확인: hasText={}, length={}",
                StringUtils.hasText(apiKey), apiKey != null ? apiKey.length() : 0);

        // 테스트용 모킹 응답 (실제 DART API 키가 없는 경우)
        if (!StringUtils.hasText(apiKey) || "your-dart-api-key".equals(apiKey)
                || "your-actual-dart-api-key-here".equals(apiKey)) {
            log.warn("[WebClientService] 실제 DART API 키가 없어 테스트용 모킹 응답 반환: corpCode={}", corpCode);

            // 삼성전자 테스트 데이터
            if ("00126380".equals(corpCode)) {
                CompanyProfileResponse mockResponse = CompanyProfileResponse.builder()
                        .status("000")
                        .corpCode("00126380")
                        .corpName("삼성전자(주)")
                        .corpNameEng("SAMSUNG ELECTRONICS CO,.LTD")
                        .stockCode("005930")
                        .stockName("삼성전자")
                        .ceoName("한종희")
                        .corpClass("Y")
                        .businessNumber("124-81-00998")
                        .corporateRegistrationNumber("130111-0006246")
                        .address("경기도 수원시 영통구 삼성로 129 (매탄동)")
                        .homepageUrl("www.samsung.com")
                        .phoneNumber("031-200-1114")
                        .industryCode("26410")
                        .establishmentDate("19690113")
                        .accountingMonth("12")
                        .build();

                log.info("[WebClientService] 테스트용 모킹 응답 생성: corpName={}, stockName={}, industryCode={}",
                        mockResponse.getCorpName(), mockResponse.getStockName(), mockResponse.getIndustryCode());
                return Mono.just(mockResponse);
            } else {
                // 다른 회사 코드에 대한 기본 모킹 응답
                CompanyProfileResponse mockResponse = CompanyProfileResponse.builder()
                        .status("000")
                        .corpCode(corpCode)
                        .corpName("테스트 회사명")
                        .stockName("테스트 종목명")
                        .industryCode("12345")
                        .build();
                return Mono.just(mockResponse);
            }
        }

        if (webClient == null) {
            log.error("[WebClientService] webClient가 null입니다! 초기화 실패 가능성. corpCode={}", corpCode);
            return Mono.error(new IllegalStateException("WebClientService가 제대로 초기화되지 않았습니다."));
        }
        if (!StringUtils.hasText(corpCode)) {
            log.error("[WebClientService] corpCode가 비어있습니다.");
            return Mono.error(new IllegalArgumentException("corpCode는 비어있을 수 없습니다."));
        }

        log.info("회사 정보 조회 API 호출 시작: corpCode={}", corpCode);

        // 실제 요청 URL 로깅 (API 키 마스킹)
        String maskedApiKey = apiKey.substring(0, Math.min(4, apiKey.length())) + "****" +
                (apiKey.length() > 8 ? apiKey.substring(apiKey.length() - 4) : "");
        String requestUrl = baseUrl + "/api/company.json?crtfc_key=" + maskedApiKey + "&corp_code=" + corpCode;
        log.info("[WebClientService] 실제 요청 URL: {}", requestUrl);
        log.info("[WebClientService] Base URL: {}, API Key Length: {}", baseUrl, apiKey.length());

        String uri = "/api/company.json?crtfc_key=" + apiKey + "&corp_code=" + corpCode;
        log.debug("회사 정보 조회 API 요청 URI: {}", uri);

        return webClient
                .get()
                .uri(uriBuilder -> {
                    log.info("[WebClientService] URI 빌더 시작: path=/api/company.json, crtfc_key=****, corp_code={}",
                            corpCode);
                    return uriBuilder
                            .path("/api/company.json")
                            .queryParam("crtfc_key", apiKey)
                            .queryParam("corp_code", corpCode)
                            .build();
                })
                .exchangeToMono(response -> {
                    HttpStatus responseStatus = (HttpStatus) response.statusCode();
                    log.info("DART API 응답 수신: corpCode={}, status={}", corpCode, responseStatus);

                    if (responseStatus.is2xxSuccessful()) {
                        log.info("[WebClientService] 200 응답 수신, 응답 본문 파싱 시작: corpCode={}", corpCode);

                        // 먼저 String으로 받아서 원시 응답 확인
                        return response.bodyToMono(String.class)
                                .doOnNext(rawResponse -> {
                                    log.info("DART API 원시 응답: corpCode={}, responseLength={}, response={}",
                                            corpCode, rawResponse.length(), rawResponse);
                                })
                                .flatMap(rawResponse -> {
                                    try {
                                        // JSON 파싱 시도
                                        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                        // 파싱 전 원시 JSON 구조 로깅
                                        log.info("JSON 파싱 시도: corpCode={}, rawJsonLength={}", corpCode,
                                                rawResponse.length());

                                        CompanyProfileResponse profile = objectMapper.readValue(rawResponse,
                                                CompanyProfileResponse.class);

                                        if (profile != null) {
                                            log.info(
                                                    "DART API 파싱 성공: corpCode={}, status={}, corpName={}, stockName={}, industryCode={}",
                                                    corpCode, profile.getStatus(), profile.getCorpName(),
                                                    profile.getStockName(), profile.getIndustryCode());

                                            // DART API 응답 상태 확인
                                            if ("000".equals(profile.getStatus())) {
                                                log.info(
                                                        "DART API 성공 응답 (status=000): corpCode={}, corpName={}, stockCode={}, stockName={}, industryCode={}",
                                                        corpCode, profile.getCorpName(), profile.getStockCode(),
                                                        profile.getStockName(), profile.getIndustryCode());
                                                return Mono.just(profile);
                                            } else {
                                                log.warn("DART API 오류 상태: corpCode={}, status={}, message={}",
                                                        corpCode, profile.getStatus(), profile.getMessage());
                                                log.warn("[WebClientService] DART API status가 000이 아님 - empty() 반환");
                                                return Mono.empty();
                                            }
                                        } else {
                                            log.warn("DART API 파싱 결과가 null: corpCode={}", corpCode);
                                            return Mono.empty();
                                        }
                                    } catch (Exception e) {
                                        log.error(
                                                "DART API 응답 파싱 실패: corpCode={}, error={}, stackTrace={}, rawResponse={}",
                                                corpCode, e.getMessage(), e.getClass().getSimpleName(), rawResponse, e);
                                        return Mono.empty();
                                    }
                                });
                    } else {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("[오류 응답 본문 없음]")
                                .flatMap(errorBody -> {
                                    log.error("DART API 오류 응답: corpCode={}, status={}, body={}", corpCode,
                                            responseStatus, errorBody);
                                    log.error("[WebClientService] HTTP 상태 코드가 2xx가 아님 - empty() 반환");
                                    return Mono.empty(); // KafkaConsumerService에서 null로 받고 처리하도록, 여기서는 empty() 반환
                                });
                    }
                })
                .timeout(Duration.ofSeconds(timeout))
                .doOnSubscribe(subscription -> log.info("DART API [company.json] 구독 시작: corpCode={}", corpCode))
                .doOnCancel(() -> log.warn("DART API [company.json] 요청 취소됨: corpCode={}", corpCode))
                .doOnError(error -> {
                    // 네트워크 오류, 타임아웃 등의 경우 여기에 해당
                    log.error(
                            "DART API [company.json] 호출 중 WebClient 오류 발생: corpCode={}, errorClass={}, errorMessage={}",
                            corpCode, error.getClass().getSimpleName(), error.getMessage());
                    log.error("[WebClientService] WebClient 오류로 인해 empty() 반환될 예정", error);
                })
                .doFinally(signalType -> {
                    log.info("DART API [company.json] 호출 완료: corpCode={}, signalType={}", corpCode, signalType);
                });
    }

    /**
     * DART API에서 공시 정보를 검색합니다.
     *
     * @param corpCode  회사 코드
     * @param startDate 검색 시작일(YYYYMMDD)
     * @param endDate   검색 종료일(YYYYMMDD)
     * @return 공시 검색 결과 응답 Mono
     */
    @RateLimiter(name = "dartApi")
    public Mono<DisclosureSearchResponse> searchDisclosures(String corpCode, String startDate, String endDate) {
        log.info("공시 검색 API 호출: {}, {} ~ {}", corpCode, startDate, endDate);

        String uri = String.format("/api/list.json?crtfc_key=%s&corp_code=%s&bgn_de=%s&end_de=%s&page_count=100",
                apiKey, corpCode, startDate, endDate);
        log.debug("공시 검색 API 요청 URI: {}", uri);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/list.json")
                        .queryParam("crtfc_key", apiKey)
                        .queryParam("corp_code", corpCode)
                        .queryParam("bgn_de", startDate)
                        .queryParam("end_de", endDate)
                        .queryParam("page_count", 100)
                        .build())
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(DisclosureSearchResponse.class);
                    } else if (response.statusCode().is4xxClientError()) {
                        log.error("공시 검색 API 클라이언트 오류: {}, 회사 코드: {}, 기간: {} ~ {}",
                                response.statusCode(), corpCode, startDate, endDate);
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("공시 검색 API 오류 응답 본문: {}", errorBody);
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST, "DART API 요청 오류: " + errorBody));
                                });
                    } else {
                        log.error("공시 검색 API 서버 오류: {}", response.statusCode());
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "DART API 서버 오류가 발생했습니다."));
                    }
                })
                .timeout(Duration.ofSeconds(timeout))
                .doOnError(error -> log.error("공시 검색 API 호출 중 오류 발생: {}", error.getMessage(), error));
    }

    /**
     * DART API에서 단일 회사 전체 재무제표를 조회합니다.
     *
     * @param corpCode  고유번호 (8자리)
     * @param bsnsYear  사업연도 (4자리)
     * @param reprtCode 보고서 코드 (1분기: 11013, 반기: 11012, 3분기: 11014, 사업보고서: 11011)
     * @param fsDiv     개별/연결 구분 (OFS: 재무제표, CFS: 연결재무제표)
     * @return 재무제표 정보 응답 Mono
     */
    @RateLimiter(name = "dartApi")
    public Mono<FinancialStatementResponseDto> getFinancialStatementApi(String corpCode, String bsnsYear,
            String reprtCode, String fsDiv) {
        log.info("단일 회사 전체 재무제표 조회 API 호출: corpCode={}, bsnsYear={}, reprtCode={}, fsDiv={}",
                corpCode, bsnsYear, reprtCode, fsDiv);

        String uri = String.format(
                "/api/fnlttSinglAcntAll.json?crtfc_key=%s&corp_code=%s&bsns_year=%s&reprt_code=%s&fs_div=%s",
                apiKey, corpCode, bsnsYear, reprtCode, fsDiv);
        log.debug("재무제표 조회 API 요청 URI: {}", uri);

        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/fnlttSinglAcntAll.json")
                        .queryParam("crtfc_key", apiKey)
                        .queryParam("corp_code", corpCode)
                        .queryParam("bsns_year", bsnsYear)
                        .queryParam("reprt_code", reprtCode)
                        .queryParam("fs_div", fsDiv)
                        .build())
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(FinancialStatementResponseDto.class);
                    } else if (response.statusCode().is4xxClientError()) {
                        log.error("재무제표 조회 API 클라이언트 오류: {}, corpCode={}, bsnsYear={}, reprtCode={}, fsDiv={}",
                                response.statusCode(), corpCode, bsnsYear, reprtCode, fsDiv);
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("재무제표 조회 API 오류 응답 본문: {}", errorBody);
                                    return Mono.error(new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST, "DART API 요청 오류: " + errorBody));
                                });
                    } else {
                        log.error("재무제표 조회 API 서버 오류: {}", response.statusCode());
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR, "DART API 서버 오류가 발생했습니다."));
                    }
                })
                .timeout(Duration.ofSeconds(timeout))
                .doOnError(error -> log.error("재무제표 조회 API 호출 중 오류 발생: {}", error.getMessage(), error));
    }
}
