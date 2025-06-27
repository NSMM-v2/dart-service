/**
 * @file CacheConfig.java
 * @description 애플리케이션의 캐시 설정을 구성합니다.
 *              Caffeine 캐시 라이브러리를 사용하여 성능을 최적화합니다.
 */
package com.nsmm.esg.dart_service.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.annotation.EnableCaching;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Value("${dart.api.cache.companyProfiles.ttl:3600}")
    private int companyProfilesTtl;

    @Value("${dart.api.cache.companyProfiles.maxSize:1000}")
    private int companyProfilesMaxSize;

    @Value("${dart.api.cache.disclosureSearch.ttl:3600}")
    private int disclosureSearchTtl;

    @Value("${dart.api.cache.disclosureSearch.maxSize:500}")
    private int disclosureSearchMaxSize;

    @Value("${dart.api.cache.dartCorpCodes.ttl:86400}")
    private int dartCorpCodesTtl;

    @Value("${dart.api.cache.dartCorpCodes.maxSize:10000}")
    private int dartCorpCodesMaxSize;

    @Primary
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        log.info("동기 CaffeineCacheManager 생성 및 asyncCacheMode 설정 시도...");
        cacheManager.setAsyncCacheMode(true);
        log.info("동기 CaffeineCacheManager asyncCacheMode 설정 완료.");

        cacheManager.registerCustomCache("companyProfiles", 
            Caffeine.newBuilder()
                .expireAfterWrite(companyProfilesTtl, TimeUnit.SECONDS)
                .maximumSize(companyProfilesMaxSize)
                .buildAsync());

        cacheManager.registerCustomCache("disclosureSearch", 
            Caffeine.newBuilder()
                .expireAfterWrite(disclosureSearchTtl, TimeUnit.SECONDS)
                .maximumSize(disclosureSearchMaxSize)
                .buildAsync());

        cacheManager.registerCustomCache("dartCorpCodes", 
            Caffeine.newBuilder()
                .expireAfterWrite(dartCorpCodesTtl, TimeUnit.SECONDS)
                .maximumSize(dartCorpCodesMaxSize)
                .buildAsync());
        return cacheManager;
    }
} 
