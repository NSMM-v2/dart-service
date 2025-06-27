/**
 * @file DisclosureRepository.java
 * @description 공시 정보에 대한 데이터베이스 액세스를 제공하는 저장소 인터페이스입니다.
 */
package com.nsmm.esg.dart_service.database.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nsmm.esg.dart_service.database.entity.Disclosure;

@Repository
public interface DisclosureRepository extends JpaRepository<Disclosure, String> {
    
    /**
     * 회사 코드로 공시 정보를 검색합니다.
     *
     * @param corpCode 회사 코드
     * @return 검색된 공시 정보 목록
     */
    List<Disclosure> findByCompanyProfile_CorpCode(String corpCode);
    
    /**
     * 회사 코드와 접수일 범위로 공시 정보를 검색합니다.
     *
     * @param corpCode 회사 코드
     * @param startDate 검색 시작일
     * @param endDate 검색 종료일
     * @return 검색된 공시 정보 목록
     */
    List<Disclosure> findByCompanyProfile_CorpCodeAndReceiptDateBetween(
            String corpCode, LocalDate startDate, LocalDate endDate);
    
    /**
     * 보고서 이름으로 공시 정보를 검색합니다.
     *
     * @param reportName 보고서 이름 키워드
     * @return 검색된 공시 정보 목록
     */
    List<Disclosure> findByReportNameContaining(String reportName);
} 