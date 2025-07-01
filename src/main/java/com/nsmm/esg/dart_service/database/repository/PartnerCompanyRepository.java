/**
 * @file PartnerCompanyRepository.java
 * @description 파트너 회사 정보에 대한 데이터베이스 액세스를 제공하는 저장소 인터페이스입니다.
 *              CompanyProfile과의 연관관계를 통해 회사 정보를 조회합니다.
 */
package com.nsmm.esg.dart_service.database.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nsmm.esg.dart_service.database.entity.PartnerCompany;
import com.nsmm.esg.dart_service.partner.model.PartnerCompanyStatus;

@Repository
public interface PartnerCompanyRepository extends JpaRepository<PartnerCompany, String> {

        /**
         * CompanyProfile의 회사명으로 파트너 회사를 검색합니다. (대소문자 구분 없음)
         *
         * @param companyName 회사명 (부분 일치)
         * @param status      파트너 회사 상태
         * @param pageable    페이지네이션 정보
         * @return 검색된 파트너 회사 목록 (페이지네이션)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE LOWER(cp.corpName) LIKE LOWER(CONCAT('%', :companyName, '%')) " +
                        "AND p.status = :status")
        Page<PartnerCompany> findByCompanyNameContainingIgnoreCaseAndStatus(
                        @Param("companyName") String companyName,
                        @Param("status") PartnerCompanyStatus status,
                        Pageable pageable);

        /**
         * 특정 본사/협력사의 파트너 회사 목록을 회사명으로 검색합니다. (대소문자 구분 없음)
         *
         * @param headquartersId 본사 ID
         * @param companyName    회사명 (부분 일치)
         * @param status         파트너 회사 상태
         * @param pageable       페이지네이션 정보
         * @return 검색된 파트너 회사 목록 (페이지네이션)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE p.headquartersId = :headquartersId " +
                        "AND LOWER(cp.corpName) LIKE LOWER(CONCAT('%', :companyName, '%')) " +
                        "AND p.status = :status")
        Page<PartnerCompany> findByHeadquartersIdAndCompanyNameContainingIgnoreCaseAndStatus(
                        @Param("headquartersId") Long headquartersId,
                        @Param("companyName") String companyName,
                        @Param("status") PartnerCompanyStatus status,
                        Pageable pageable);

        /**
         * 특정 협력사 사용자의 파트너 회사 목록을 회사명으로 검색합니다. (대소문자 구분 없음)
         *
         * @param partnerId   협력사 사용자 ID
         * @param companyName 회사명 (부분 일치)
         * @param status      파트너 회사 상태
         * @param pageable    페이지네이션 정보
         * @return 검색된 파트너 회사 목록 (페이지네이션)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE p.partnerId = :partnerId " +
                        "AND LOWER(cp.corpName) LIKE LOWER(CONCAT('%', :companyName, '%')) " +
                        "AND p.status = :status")
        Page<PartnerCompany> findByPartnerIdAndCompanyNameContainingIgnoreCaseAndStatus(
                        @Param("partnerId") Long partnerId,
                        @Param("companyName") String companyName,
                        @Param("status") PartnerCompanyStatus status,
                        Pageable pageable);

        /**
         * 특정 본사의 파트너 회사 목록을 상태별로 검색합니다.
         *
         * @param headquartersId 본사 ID
         * @param status         파트너 회사 상태
         * @param pageable       페이지네이션 정보
         * @return 검색된 파트너 회사 목록 (페이지네이션)
         */
        Page<PartnerCompany> findByHeadquartersIdAndStatus(Long headquartersId, PartnerCompanyStatus status,
                        Pageable pageable);

        /**
         * 특정 협력사 사용자의 파트너 회사 목록을 상태별로 검색합니다.
         *
         * @param partnerId 협력사 사용자 ID
         * @param status    파트너 회사 상태
         * @param pageable  페이지네이션 정보
         * @return 검색된 파트너 회사 목록 (페이지네이션)
         */
        Page<PartnerCompany> findByPartnerIdAndStatus(Long partnerId, PartnerCompanyStatus status, Pageable pageable);

        /**
         * 상태별로 파트너 회사를 검색합니다.
         *
         * @param status   파트너 회사 상태
         * @param pageable 페이지네이션 정보
         * @return 검색된 파트너 회사 목록 (페이지네이션)
         */
        Page<PartnerCompany> findByStatus(PartnerCompanyStatus status, Pageable pageable);

        /**
         * 상태별로 모든 파트너 회사를 검색합니다.
         *
         * @param status 파트너 회사 상태
         * @return 검색된 파트너 회사 목록
         */
        java.util.List<PartnerCompany> findByStatus(PartnerCompanyStatus status);

        /**
         * DART 기업 코드로 파트너 회사를 검색합니다.
         *
         * @param corpCode DART 기업 코드
         * @return 검색된 파트너 회사 (Optional)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp WHERE cp.corpCode = :corpCode")
        Optional<PartnerCompany> findByCorpCode(@Param("corpCode") String corpCode);

        /**
         * ID와 상태로 파트너 회사를 검색합니다.
         *
         * @param id     파트너 회사 ID
         * @param status 파트너 회사 상태
         * @return 검색된 파트너 회사 (Optional)
         */
        Optional<PartnerCompany> findByIdAndStatus(String id, PartnerCompanyStatus status);

        /**
         * CompanyProfile의 회사명으로 파트너 회사를 검색합니다. (모든 상태 포함, 대소문자 구분 없음)
         *
         * @param companyName 회사명 (부분 일치)
         * @param pageable    페이지네이션 정보
         * @return 검색된 파트너 회사 목록 (페이지네이션)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE LOWER(cp.corpName) LIKE LOWER(CONCAT('%', :companyName, '%'))")
        Page<PartnerCompany> findByCompanyNameContainingIgnoreCase(
                        @Param("companyName") String companyName,
                        Pageable pageable);

        /**
         * CompanyProfile의 회사명과 상태로 파트너 회사를 검색합니다. (정확히 일치, 대소문자 구분 없음)
         *
         * @param companyName 회사명 (정확히 일치)
         * @param status      파트너 회사 상태
         * @return 검색된 파트너 회사 (Optional)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE LOWER(cp.corpName) = LOWER(:companyName) " +
                        "AND p.status = :status")
        Optional<PartnerCompany> findByCompanyNameIgnoreCaseAndStatus(
                        @Param("companyName") String companyName,
                        @Param("status") PartnerCompanyStatus status);

        /**
         * CompanyProfile의 회사명으로 활성 상태의 파트너 회사가 존재하는지 확인합니다. (정확히 일치, 대소문자 구분 없음)
         *
         * @param companyName 회사명 (정확히 일치)
         * @return 존재 여부
         */
        @Query("SELECT COUNT(p) > 0 FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE LOWER(cp.corpName) = LOWER(:companyName) " +
                        "AND p.status = :status")
        boolean existsByCompanyNameIgnoreCaseAndStatus(
                        @Param("companyName") String companyName,
                        @Param("status") PartnerCompanyStatus status);

        /**
         * 본사 ID로 파트너 회사를 검색합니다.
         *
         * @param headquartersId 본사 ID
         * @return 검색된 파트너 회사 목록
         */
        java.util.List<PartnerCompany> findByHeadquartersId(Long headquartersId);

        /**
         * 협력사 사용자 ID로 파트너 회사를 검색합니다.
         *
         * @param partnerId 협력사 사용자 ID
         * @return 검색된 파트너 회사 목록
         */
        java.util.List<PartnerCompany> findByPartnerId(Long partnerId);

        /**
         * 본사 ID와 법인등록번호로 파트너 회사를 검색합니다.
         *
         * @param headquartersId 본사 ID
         * @param corpCode       법인등록번호
         * @return 검색된 파트너 회사 (Optional)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE p.headquartersId = :headquartersId AND cp.corpCode = :corpCode")
        Optional<PartnerCompany> findByHeadquartersIdAndCorpCode(
                        @Param("headquartersId") Long headquartersId,
                        @Param("corpCode") String corpCode);

        /**
         * 협력사 ID와 법인등록번호로 파트너 회사를 검색합니다.
         *
         * @param partnerId 협력사 ID
         * @param corpCode  법인등록번호
         * @return 검색된 파트너 회사 (Optional)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE p.partnerId = :partnerId AND cp.corpCode = :corpCode")
        Optional<PartnerCompany> findByPartnerIdAndCorpCode(
                        @Param("partnerId") Long partnerId,
                        @Param("corpCode") String corpCode);

        /**
         * 특정 본사의 회사명으로 파트너 회사를 검색합니다. (정확히 일치, 대소문자 구분 없음)
         *
         * @param headquartersId 본사 ID
         * @param companyName    회사명 (정확히 일치)
         * @param status         파트너 회사 상태
         * @return 검색된 파트너 회사 (Optional)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE p.headquartersId = :headquartersId " +
                        "AND LOWER(cp.corpName) = LOWER(:companyName) " +
                        "AND p.status = :status")
        Optional<PartnerCompany> findByHeadquartersIdAndCompanyNameIgnoreCaseAndStatus(
                        @Param("headquartersId") Long headquartersId,
                        @Param("companyName") String companyName,
                        @Param("status") PartnerCompanyStatus status);

        /**
         * 특정 협력사의 회사명으로 파트너 회사를 검색합니다. (정확히 일치, 대소문자 구분 없음)
         *
         * @param partnerId   협력사 ID
         * @param companyName 회사명 (정확히 일치)
         * @param status      파트너 회사 상태
         * @return 검색된 파트너 회사 (Optional)
         */
        @Query("SELECT p FROM PartnerCompany p JOIN p.companyProfile cp " +
                        "WHERE p.partnerId = :partnerId " +
                        "AND LOWER(cp.corpName) = LOWER(:companyName) " +
                        "AND p.status = :status")
        Optional<PartnerCompany> findByPartnerIdAndCompanyNameIgnoreCaseAndStatus(
                        @Param("partnerId") Long partnerId,
                        @Param("companyName") String companyName,
                        @Param("status") PartnerCompanyStatus status);

}