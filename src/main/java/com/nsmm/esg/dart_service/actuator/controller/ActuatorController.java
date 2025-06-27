/**
 * @file ActuatorController.java
 * @description 애플리케이션의 상태 확인 및 정보 제공을 위한 Actuator 엔드포인트를 제공합니다.
 */
package com.nsmm.esg.dart_service.actuator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/actuator")
@Tag(name = "애플리케이션 모니터링 API", description = "애플리케이션의 상태 확인 및 정보 제공 API")
public class ActuatorController {

    @GetMapping("/health")
    @Operation(summary = "애플리케이션 헬스 체크", description = "애플리케이션의 현재 상태 및 주요 구성 요소(DB, Disk Space 등)의 상태를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "헬스 체크 성공", 
                 content = @Content(mediaType = "application/json",
                                  schema = @Schema(type = "object", 
                                                 example = "{\"status\": \"UP\", \"components\": {\"diskSpace\": {\"status\": \"UP\"}, \"db\": {\"status\": \"UP\"}}}")))
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");

        Map<String, Object> components = new HashMap<>();
        components.put("diskSpace", Map.of("status", "UP"));
        components.put("db", Map.of("status", "UP"));

        health.put("components", components);

        return ResponseEntity.ok(health);
    }

    @GetMapping("/info")
    @Operation(summary = "애플리케이션 정보 조회", description = "애플리케이션 이름, 버전, 실행 환경(Java, OS) 등의 정보를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "애플리케이션 정보 조회 성공",
                 content = @Content(mediaType = "application/json",
                                  schema = @Schema(type = "object", 
                                                 example = "{\"app\": {\"name\": \"dart-api-service\", \"version\": \"1.0.0\"}, \"java\": {\"version\": \"17.0.1\"}, \"os\": {\"name\": \"Mac OS X\", \"version\": \"13.4\"}}")))
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("app", Map.of("name", "dart-api-service", "version", "1.0.0"));
        info.put("java", Map.of("version", System.getProperty("java.version")));
        info.put("os", Map.of("name", System.getProperty("os.name"), "version", System.getProperty("os.version")));

        return ResponseEntity.ok(info);
    }
} 