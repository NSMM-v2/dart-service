/**
 * @file FinancialStatementDataRepository.java
 * @description 재무제표 데이터(`FinancialStatementData`) 엔티티에 대한 데이터베이스 연산을 처리하는 Spring Data JPA 리포지토리입니다.
 *              기업 코드, 사업연도, 보고서 코드를 기반으로 재무 데이터를 조회하고 삭제하는 기능을 제공합니다.
 */
package com.nsmm.esg.dart_service.database.repository;

import com.nsmm.esg.dart_service.database.entity.FinancialStatementData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialStatementDataRepository extends JpaRepository<FinancialStatementData, Long> {

    /**
     * 회사 코드, 사업연도, 보고서 코드로 재무제표 항목 리스트를 조회합니다.
     *
     * @param corpCode  회사 고유번호
     * @param bsnsYear  사업 연도
     * @param reprtCode 보고서 코드
     * @return 재무제표 항목 리스트
     */
    List<FinancialStatementData> findByCorpCodeAndBsnsYearAndReprtCode(String corpCode, String bsnsYear,
            String reprtCode);


    /**
     * 특정 회사의 재무제표 데이터에서 고유한 연도/분기 조합과 항목 수를 조회합니다.
     * 
     * @param corpCode 회사 고유번호
     * @return [사업연도, 보고서코드, 항목수] 형태의 Object 배열 리스트
     */
    @Query("SELECT f.bsnsYear, f.reprtCode, COUNT(f) " +
            "FROM FinancialStatementData f " +
            "WHERE f.corpCode = :corpCode " +
            "GROUP BY f.bsnsYear, f.reprtCode " +
            "ORDER BY f.bsnsYear DESC, f.reprtCode DESC")
    List<Object[]> findDistinctYearAndReportByCorpCode(@Param("corpCode") String corpCode);

}