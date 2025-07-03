/**
 * @file DartApiController.java
 * @description DART API 기능을 REST API 형태로 제공하는 컨트롤러입니다.
 */
package com.nsmm.esg.dart_service.dart.controller;

import com.nsmm.esg.dart_service.dart.dto.CompanyProfileResponse;
import com.nsmm.esg.dart_service.dart.dto.CorpCodeQueryDto;
import com.nsmm.esg.dart_service.dart.dto.DisclosureSearchResponse;
import com.nsmm.esg.dart_service.database.entity.DartCorpCode;
import com.nsmm.esg.dart_service.dart.service.DartApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/dart")
@Tag(name = "DART API", description = "DART Open API 기능을 제공하는 API")
@RequiredArgsConstructor
@Slf4j
public class DartApiController {

        private final DartApiService dartApiService;

        @GetMapping("/company/{corpCode}")
        @Operation(summary = "회사 개황 정보 조회", description = "DART 고유번호(corpCode)를 이용하여 특정 회사의 상세 개황 정보를 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "회사 개황 정보 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CompanyProfileResponse.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 형식 (예: 유효하지 않은 corpCode)"),
                        @ApiResponse(responseCode = "404", description = "해당 회사 정보를 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public Mono<ResponseEntity<CompanyProfileResponse>> getCompanyProfile(
                        @Parameter(description = "조회할 회사의 DART 고유번호 (8자리 숫자)", required = true, example = "00126380") @PathVariable String corpCode) {

                log.info("회사 정보 조회 API 요청 - 회사 코드: {}", corpCode);
                return dartApiService.getCompanyProfile(corpCode)
                                .map(ResponseEntity::ok)
                                .doOnError(e -> log.error("회사 정보 조회 중 오류 발생: {}", e.getMessage(), e));
        }

        @GetMapping("/disclosures")
        @Operation(summary = "공시 정보 검색", description = "DART 고유번호(corpCode)와 기간(시작일, 종료일)을 기준으로 공시 정보를 검색합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "공시 정보 검색 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DisclosureSearchResponse.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 형식 (예: 날짜 형식 오류)"),
                        @ApiResponse(responseCode = "404", description = "해당 조건의 공시 정보를 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public Mono<ResponseEntity<DisclosureSearchResponse>> searchDisclosures(
                        @Parameter(description = "조회할 회사의 DART 고유번호 (8자리 숫자)", required = true, example = "00126380") @RequestParam String corpCode,

                        @Parameter(description = "검색 시작일 (YYYYMMDD 형식)", required = true, example = "20230101") @RequestParam String startDate,

                        @Parameter(description = "검색 종료일 (YYYYMMDD 형식)", required = true, example = "20231231") @RequestParam String endDate) {

                log.info("공시 정보 검색 API 요청 - 회사 코드: {}, 기간: {} ~ {}", corpCode, startDate, endDate);
                return dartApiService.searchDisclosures(corpCode, startDate, endDate)
                                .map(ResponseEntity::ok)
                                .doOnError(e -> log.error("공시 정보 검색 중 오류 발생: {}", e.getMessage(), e));
        }

        @PostMapping("/corp-codes/sync")
        @Operation(summary = "DART 기업 코드 동기화", description = "DART API로부터 모든 기업 코드를 다운로드하여 로컬 데이터베이스와 동기화합니다. 이 작업은 비동기로 처리됩니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "202", description = "기업 코드 동기화 작업 시작됨 (비동기 처리)"),
                        @ApiResponse(responseCode = "500", description = "동기화 작업 시작 중 오류 발생")
        })
        public ResponseEntity<Void> syncCorpCodes() {
                log.info("DART 기업 코드 동기화 요청 수신");
                dartApiService.fetchAndStoreCorpCodes().subscribe(
                                null, // onNext (결과 무시)
                                error -> log.error("기업 코드 동기화 중 비동기 오류 발생", error),
                                () -> log.info("기업 코드 동기화 작업 완료 (비동기)"));
                // 실제 작업은 비동기로 수행되므로 즉시 202 Accepted 반환
                return ResponseEntity.accepted().build();
        }

        @GetMapping("/corp-codes")
        @Operation(summary = "저장된 DART 기업 코드 목록 조회", description = "로컬 데이터베이스에 저장된 DART 기업 코드 목록을 페이지네이션 및 필터링하여 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "기업 코드 목록 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))), // 실제로는
                                                                                                                                                                                               // Page<DartCorpCode>이지만,
                                                                                                                                                                                               // Swagger
                                                                                                                                                                                               // 표현
                                                                                                                                                                                               // 간소화
                        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<Page<DartCorpCode>> getAllCorpCodes(
                        @Parameter(description = "기업 코드 조회 조건 DTO", schema = @Schema(implementation = CorpCodeQueryDto.class)) CorpCodeQueryDto queryDto) {
                log.info("저장된 DART 기업 코드 목록 조회 요청: {}", queryDto);
                Page<DartCorpCode> corpCodes = dartApiService.getAllCorpCodes(queryDto);
                return ResponseEntity.ok(corpCodes);
        }

        @GetMapping("/corp-codes/{corpCode}")
        @Operation(summary = "특정 DART 기업 코드 조회 (고유번호)", description = "DART 고유번호(corp_code)를 사용하여 로컬 데이터베이스에 저장된 특정 DART 기업 코드를 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "기업 코드 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DartCorpCode.class))),
                        @ApiResponse(responseCode = "404", description = "해당 고유번호의 기업 코드를 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<DartCorpCode> findCorpCodeByCorpCode(
                        @Parameter(description = "조회할 DART 고유번호 (8자리 숫자)", required = true, example = "00126380") @PathVariable String corpCode) {
                log.info("DART 기업 코드 조회 (고유번호): {}", corpCode);
                Optional<DartCorpCode> corpCodeOpt = dartApiService.findCorpCodeByCorpCode(corpCode);
                return corpCodeOpt.map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/corp-codes/stock/{stockCode}")
        @Operation(summary = "특정 DART 기업 코드 조회 (종목코드)", description = "주식 종목코드(stock_code)를 사용하여 로컬 데이터베이스에 저장된 특정 DART 기업 코드를 조회합니다. (상장사인 경우)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "기업 코드 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DartCorpCode.class))),
                        @ApiResponse(responseCode = "404", description = "해당 종목코드의 기업 코드를 찾을 수 없음"),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<DartCorpCode> findCorpCodeByStockCode(
                        @Parameter(description = "조회할 주식 종목 코드 (6자리 숫자 또는 문자 조합)", required = true, example = "005930") @PathVariable String stockCode) {
                log.info("DART 기업 코드 조회 (종목코드): {}", stockCode);
                Optional<DartCorpCode> corpCodeOpt = dartApiService.findCorpCodeByStockCode(stockCode);
                return corpCodeOpt.map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/corp-codes/search")
        @Operation(summary = "회사명으로 DART 기업 코드 검색", description = "회사명을 기준으로 로컬 데이터베이스에 저장된 DART 기업 코드를 검색합니다. (부분 일치)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "기업 코드 검색 성공", content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = DartCorpCode.class))),
                        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
        })
        public ResponseEntity<List<DartCorpCode>> searchCorpCodesByName(
                        @Parameter(description = "검색할 회사명 (부분 일치 가능)", required = true, example = "삼성전자") @RequestParam String corpName) {
                log.info("회사명으로 DART 기업 코드 검색: {}", corpName);
                List<DartCorpCode> corpCodes = dartApiService.searchCorpCodesByName(corpName);
                return ResponseEntity.ok(corpCodes);
        }

        @GetMapping("/debug/company/{corpCode}")
        @Operation(summary = "[디버깅] 회사 정보 조회 및 상세 로깅", description = "회사 정보 조회 시 상세한 로그와 함께 DART API 응답을 확인합니다.")
        public Mono<ResponseEntity<String>> debugCompanyProfile(
                        @Parameter(description = "조회할 회사의 DART 고유번호 (8자리 숫자)", required = true, example = "00126380") @PathVariable String corpCode) {

                log.info("[DEBUG] 회사 정보 디버깅 조회 시작: corpCode={}", corpCode);

                return dartApiService.getCompanyProfile(corpCode)
                                .map(profile -> {
                                        String debugInfo = String.format(
                                                        "DEBUG INFO - corpCode: %s, status: %s, corpName: %s, stockName: %s, industryCode: %s",
                                                        corpCode, profile.getStatus(), profile.getCorpName(),
                                                        profile.getStockName(), profile.getIndustryCode());

                                        return ResponseEntity.ok(debugInfo);
                                })
                                .switchIfEmpty(Mono.fromSupplier(() -> {
                                        log.warn("[DEBUG] DART API 응답이 비어있음: corpCode={}", corpCode);
                                        return ResponseEntity.ok("DEBUG: API 응답이 비어있음 - corpCode: " + corpCode);
                                }))
                                .onErrorResume(error -> {
                                        log.error("[DEBUG] DART API 호출 중 오류 발생: corpCode={}", corpCode, error);
                                        String errorInfo = String.format(
                                                        "DEBUG ERROR - corpCode: %s, error: %s, message: %s",
                                                        corpCode, error.getClass().getSimpleName(), error.getMessage());
                                        return Mono.just(ResponseEntity.ok(errorInfo));
                                });
        }

}
