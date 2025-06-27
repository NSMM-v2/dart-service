/**
 * @file CompanyProfileRepository.java
 * @description 회사 정보에 대한 데이터베이스 액세스를 제공하는 저장소 인터페이스입니다.
 */
package com.nsmm.esg.dart_service.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nsmm.esg.dart_service.database.entity.CompanyProfile;

@Repository
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, String> {
    
    /**
     * 회사명으로 회사 정보를 검색합니다.
     *
     * @param corpName 회사명
     * @return 검색된 회사 정보 목록
     */
    List<CompanyProfile> findByCorpNameContaining(String corpName);
    
    /**
     * 종목 코드로 회사 정보를 검색합니다.
     *
     * @param stockCode 종목 코드
     * @return 검색된 회사 정보 (Optional)
     */
    Optional<CompanyProfile> findByStockCode(String stockCode);
    
    /**
     * 회사 분류별로 회사 정보를 검색합니다.
     *
     * @param corpClass 회사 분류
     * @return 검색된 회사 정보 목록
     */
    List<CompanyProfile> findByCorpClass(String corpClass);
} 