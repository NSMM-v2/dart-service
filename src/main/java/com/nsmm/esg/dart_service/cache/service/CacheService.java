/**
 * @file CacheService.java
 * @description 애플리케이션 전반의 캐싱 기능을 제공하는 서비스 클래스입니다.
 *              캐싱을 통해 자주 사용되는 데이터의 접근 속도를 개선하고 시스템 부하를 줄입니다.
 */
package com.nsmm.esg.dart_service.cache.service;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    private final CacheManager cacheManager;

    /**
     * 캐시에서 데이터를 가져옵니다.
     * @param cacheName 캐시 이름
     * @param key 캐시 키
     * @return 캐시된 데이터 또는 null (캐시 미스)
     */
    public <T> T get(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(key);
            if (valueWrapper != null) {
                log.debug("캐시 히트: {}:{}", cacheName, key);
                return (T) valueWrapper.get();
            }
        }
        log.debug("캐시 미스: {}:{}", cacheName, key);
        return null;
    }

    /**
     * 데이터를 캐시에 저장합니다.
     * @param cacheName 캐시 이름
     * @param key 캐시 키
     * @param value 저장할 데이터
     */
    public <T> void put(String cacheName, Object key, T value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
            log.debug("캐시 저장: {}:{}", cacheName, key);
        } else {
            log.warn("캐시 저장 실패: {}:{} - 캐시가 존재하지 않습니다", cacheName, key);
        }
    }

    /**
     * 캐시에서 특정 키의 데이터를 삭제합니다.
     * @param cacheName 캐시 이름
     * @param key 삭제할 캐시 키
     */
    public void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("캐시 삭제: {}:{}", cacheName, key);
        }
    }

    /**
     * 특정 캐시의 모든 항목을 삭제합니다.
     * @param cacheName 캐시 이름
     */
    public void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("캐시 전체 삭제: {}", cacheName);
        }
    }

    /**
     * 캐시가 존재하면 반환하고, 없으면 제공된 함수를 실행하여 결과를 캐시한 후 반환합니다.
     * @param cacheName 캐시 이름
     * @param key 캐시 키
     * @param valueLoader 캐시 미스 시 실행할 함수
     * @return 캐시된 데이터 또는 함수의 실행 결과
     */
    public <T> T getOrLoad(String cacheName, Object key, Callable<T> valueLoader) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                return cache.get(key, valueLoader);
            } else {
                log.warn("캐시 getOrLoad 실패: {}:{} - 캐시가 존재하지 않습니다", cacheName, key);
                return valueLoader.call();
            }
        } catch (Exception e) {
            log.error("캐시 getOrLoad 중 오류 발생: {}:{} - {}", cacheName, key, e.getMessage(), e);
            throw new RuntimeException("캐시 로딩 중 오류 발생", e);
        }
    }
}