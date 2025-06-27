/**
 * @file JavaVersionApplication.java
 * @description 애플리케이션의 주요 진입점입니다.
 *              Spring Boot 애플리케이션을 시작하고 기본 설정을 로드합니다.
 */
package com.example.javaversion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableScheduling
public class DartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DartServiceApplication.class, args);
    }

}
