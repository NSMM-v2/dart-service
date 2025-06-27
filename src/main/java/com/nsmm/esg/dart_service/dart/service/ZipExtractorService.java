/**
 * @file ZipExtractorService.java
 * @description ZIP 파일 추출을 담당하는 서비스 클래스입니다.
 *              DART API로부터 받은 ZIP 파일을 처리합니다.
 */
package com.nsmm.esg.dart_service.dart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class ZipExtractorService {

    /**
     * ZIP 파일에서 XML 내용을 추출합니다.
     *
     * @param dataBufferFlux ZIP 파일 데이터 버퍼 Flux
     * @return 추출된 XML 내용
     */
    public Mono<String> extractXmlFromZip(Flux<DataBuffer> dataBufferFlux) {
        return dataBufferFlux
                .collectList()
                .flatMap(dataBuffers -> {
                    // 모든 DataBuffer를 하나의 바이트 배열로 결합
                    byte[] zipBytes = combineDataBuffers(dataBuffers);
                    
                    // 결합된 데이터 버퍼 릴리스
                    dataBuffers.forEach(DataBufferUtils::release);
                    
                    // 빈 ZIP 파일 체크
                    if (zipBytes.length == 0) {
                        log.warn("DART API로부터 받은 ZIP 데이터가 비어있습니다.");
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, 
                            "DART API returned empty ZIP data."));
                    }
                    
                    // ZIP 파일에서 XML 추출
                    return extractXmlFromZipBytes(zipBytes);
                });
    }
    
    /**
     * 여러 DataBuffer를 하나의 바이트 배열로 결합합니다.
     *
     * @param dataBuffers DataBuffer 목록
     * @return 결합된 바이트 배열
     */
    private byte[] combineDataBuffers(java.util.List<DataBuffer> dataBuffers) {
        int totalSize = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
        byte[] result = new byte[totalSize];
        int offset = 0;
        
        for (DataBuffer dataBuffer : dataBuffers) {
            int length = dataBuffer.readableByteCount();
            dataBuffer.read(result, offset, length);
            offset += length;
        }
        
        return result;
    }
    
    /**
     * ZIP 바이트 배열에서 XML 내용을 추출합니다.
     *
     * @param zipBytes ZIP 파일 바이트 배열
     * @return 추출된 XML 내용
     */
    private Mono<String> extractXmlFromZipBytes(byte[] zipBytes) {
        String xmlContent;
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes));
             Scanner scanner = new Scanner(zis, StandardCharsets.UTF_8)) {
            
            // ZIP 파일의 첫 번째 엔트리로 이동 (CORPCODE.xml)
            zis.getNextEntry();
            
            // 스캐너를 사용하여 XML 내용 읽기
            if (scanner.hasNext()) {
                xmlContent = scanner.useDelimiter("\\A").next();
                
                // XML 내용 로깅
                logXmlContent(xmlContent);
                
                return Mono.just(xmlContent);
            } else {
                log.warn("ZIP 파일에서 XML 내용을 찾을 수 없습니다.");
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, 
                    "No content found in ZIP file entry."));
            }
        } catch (IOException e) {
            log.error("ZIP 파일 처리 중 오류 발생", e);
            return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error processing ZIP file: " + e.getMessage(), e));
        }
    }
    
    /**
     * XML 내용을 로깅합니다.
     *
     * @param xmlContent XML 문자열
     */
    private void logXmlContent(String xmlContent) {
        if (xmlContent != null && !xmlContent.isEmpty()) {
            log.info("ZIP 파일에서 추출한 XML 내용 (첫 2000자):\n{}",
                    xmlContent.substring(0, Math.min(xmlContent.length(), 2000)));
            if (xmlContent.length() > 2000) {
                log.info("... (XML 내용이 너무 길어 일부만 표시) ...");
            }
        } else {
            log.warn("추출된 XML 내용이 비어있거나 null입니다.");
        }
    }
}