/**
 * @file XmlParserService.java
 * @description XML 파싱을 담당하는 서비스 클래스입니다.
 *              DART API로부터 받은 XML 응답을 파싱하는 기능을 제공합니다.
 */
package com.nsmm.esg.dart_service.dart.service;

import com.nsmm.esg.dart_service.dart.dto.DartCorpCodeRootXmlDto;
import com.nsmm.esg.dart_service.dart.dto.DartCorpCodeXmlDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class XmlParserService {

    private final XmlMapper xmlMapper = new XmlMapper();

    @PostConstruct
    public void init() {
        // XML 매퍼 설정 강화
        xmlMapper.registerModule(new JavaTimeModule());
        // FAIL_ON_UNKNOWN_PROPERTIES를 false로 설정하여 알 수 없는 속성이 있어도 파싱 실패하지 않도록 함
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // XML 내용이 비어있거나 null인 경우에도 파싱 실패하지 않도록 설정
        xmlMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    /**
     * XML 문자열을 파싱하여 기업 코드 정보를 추출합니다.
     * 여러 형식의 XML을 처리할 수 있도록 다양한 파싱 전략을 시도합니다.
     *
     * @param xmlContent XML 문자열
     * @return 파싱 결과 (상태, 메시지, 기업 코드 목록)
     */
    public ParseResult parseCorpCodeXml(String xmlContent) {
        if (xmlContent == null || xmlContent.isEmpty()) {
            log.warn("XML 내용이 비어있거나 null입니다.");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, 
                "DART API returned empty or null XML content for corpCode.xml");
        }

        // 로깅 - XML 내용 일부 출력
        logXmlContent(xmlContent);

        // 먼저 기본 형식(result 요소 내부에 status와 message가 있는 경우)으로 파싱 시도
        DartCorpCodeXmlDto corpCodeDto;
        DartCorpCodeRootXmlDto rootCorpCodeDto;
        String status = "N/A";
        String message = "DART API 응답 없음 또는 형식 오류";
        List<?> corpList;

        try {
            log.debug("기본 XML 형식으로 파싱 시도 (DartCorpCodeXmlDto)");
            corpCodeDto = xmlMapper.readValue(xmlContent, DartCorpCodeXmlDto.class);
            if (corpCodeDto != null) {
                log.debug("기본 XML 형식 파싱 성공: status={}, message={}, list size={}", 
                    corpCodeDto.getStatus(), corpCodeDto.getMessage(), 
                    corpCodeDto.getList() != null ? corpCodeDto.getList().size() : "null");
                status = corpCodeDto.getStatus();
                message = corpCodeDto.getMessage() != null ? corpCodeDto.getMessage() : message;
                corpList = corpCodeDto.getList();
                return new ParseResult(status, message, corpList);
            } else {
                log.warn("기본 XML 형식 파싱 결과가 null입니다.");
            }
        } catch (Exception e) {
            log.warn("기본 XML 형식으로 파싱 실패, 대체 형식으로 시도합니다: {}", e.getMessage());
            
            // 기본 형식 파싱 실패 시 대체 형식(루트 레벨에 status와 message가 있는 경우)으로 시도
            try {
                log.debug("대체 XML 형식으로 파싱 시도 (DartCorpCodeRootXmlDto)");
                rootCorpCodeDto = xmlMapper.readValue(xmlContent, DartCorpCodeRootXmlDto.class);
                if (rootCorpCodeDto != null) {
                    log.debug("대체 XML 형식 파싱 성공: status={}, message={}, list size={}", 
                        rootCorpCodeDto.getStatus(), rootCorpCodeDto.getMessage(), 
                        rootCorpCodeDto.getList() != null ? rootCorpCodeDto.getList().size() : "null");
                    status = rootCorpCodeDto.getStatus();
                    message = rootCorpCodeDto.getMessage() != null ? rootCorpCodeDto.getMessage() : message;
                    corpList = rootCorpCodeDto.getList();
                    return new ParseResult(status, message, corpList);
                } else {
                    log.warn("대체 XML 형식 파싱 결과가 null입니다.");
                }
            } catch (Exception e2) {
                log.error("대체 XML 형식으로도 파싱 실패: {}", e2.getMessage());
                
                // 두 형식 모두 파싱 실패 시 더 자세한 오류 메시지와 함께 예외 생성
                String errorMsg = "XML 파싱 실패: 기본 형식 오류 - " + e.getMessage() + 
                                 ", 대체 형식 오류 - " + e2.getMessage();
                log.error(errorMsg);

                // XML 내용 분석을 위한 추가 로깅
                analyzeXmlStructure(xmlContent);
                
                // 마지막 시도: 일반 Map으로 파싱 시도 (더 유연한 방식)
                try {
                    log.debug("마지막 시도: 일반 Map으로 XML 파싱 시도");
                    Map genericMap = xmlMapper.readValue(xmlContent, Map.class);
                    log.debug("일반 Map으로 파싱 성공: {}", genericMap.keySet());
                    
                    // Map에서 필요한 정보 추출 시도
                    if (genericMap.containsKey("result")) {
                        Map<String, Object> resultMap = (Map<String, Object>) genericMap.get("result");
                        if (resultMap.containsKey("list")) {
                            corpList = (List<?>) resultMap.get("list");
                            status = resultMap.containsKey("status") ? resultMap.get("status").toString() : status;
                            message = resultMap.containsKey("message") ? resultMap.get("message").toString() : message;
                            return new ParseResult(status, message, corpList);
                        }
                    } else if (genericMap.containsKey("status") && genericMap.containsKey("list")) {
                        status = genericMap.get("status").toString();
                        message = genericMap.containsKey("message") ? genericMap.get("message").toString() : message;
                        corpList = (List<?>) genericMap.get("list");
                        return new ParseResult(status, message, corpList);
                    }
                    
                    log.warn("일반 Map으로 파싱은 성공했으나 필요한 데이터를 찾을 수 없습니다: {}", genericMap.keySet());
                } catch (Exception e3) {
                    log.error("일반 Map으로도 파싱 실패: {}", e3.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                        "XML 파싱 실패: 모든 파싱 방법 실패 - " + errorMsg + ", Map 파싱 오류 - " + e3.getMessage());
                }
            }
        }
        
        // 모든 파싱 시도가 실패한 경우
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
            "XML 파싱 실패: 모든 파싱 방법이 실패했습니다.");
    }
    
    /**
     * XML 내용을 로깅합니다.
     * 
     * @param xmlContent XML 문자열
     */
    private void logXmlContent(String xmlContent) {
        if (xmlContent != null && !xmlContent.isEmpty()) {
            log.info("DART API로부터 받은 XML 내용 (첫 2000자):\n{}",
                    xmlContent.substring(0, Math.min(xmlContent.length(), 2000)));
            if (xmlContent.length() > 2000) {
                log.info("... (XML 내용이 너무 길어 일부만 표시) ...");
            }
        }
    }
    
    /**
     * XML 구조를 분석하고 로깅합니다.
     * 
     * @param xmlContent XML 문자열
     */
    private void analyzeXmlStructure(String xmlContent) {
        try {
            log.debug("XML 구조 분석 시도: {}", xmlContent.substring(0, Math.min(xmlContent.length(), 500)));
            // XML 루트 요소 확인
            if (xmlContent.contains("<result>")) {
                log.debug("XML에 <result> 태그가 포함되어 있습니다.");
            } else if (xmlContent.contains("<root>")) {
                log.debug("XML에 <root> 태그가 포함되어 있습니다.");
            } else {
                log.debug("XML에 <result> 또는 <root> 태그가 없습니다. 첫 500자: {}", 
                    xmlContent.substring(0, Math.min(xmlContent.length(), 500)));
            }
        } catch (Exception e) {
            log.error("XML 구조 분석 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * XML 파싱 결과를 담는 클래스
     */
    @Getter
    public static class ParseResult {
        private final String status;
        private final String message;
        private final List<?> corpList;
        
        public ParseResult(String status, String message, List<?> corpList) {
            this.status = status;
            this.message = message;
            this.corpList = corpList;
        }

    }
}