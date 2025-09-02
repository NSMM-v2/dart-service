/**
 * @file OpenApiConfig.java
 * @description OpenAPI(Swagger) 문서화 설정 클래스입니다.
 */
package com.nsmm.esg.dart_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title(applicationName + " API 문서")
                .description("DART Open API를 활용한 기업 정보 제공 서비스 API 문서")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("API Support")
                        .email("support@example.com")
                        .url("https://example.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));

        // API 키 인증 설정
        SecurityScheme apiKeyScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-KEY");

        return new OpenAPI()
                .info(info)
                .components(new Components()
                        .addSecuritySchemes("api_key", apiKeyScheme))
                .addSecurityItem(new SecurityRequirement().addList("api_key"));
    }
} 