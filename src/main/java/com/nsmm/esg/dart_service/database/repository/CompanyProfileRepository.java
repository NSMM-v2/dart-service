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
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {


    Optional<CompanyProfile> findByCorpCode(String corpCode);

    /**
     * 본사와 법인등록번호로 회사 정보를 검색합니다.
     *
     * @param headquartersId 본사 ID
     * @param corpCode       법인등록번호
     * @return 검색된 회사 정보 (Optional)
     */
    Optional<CompanyProfile> findByHeadquartersIdAndCorpCode(Long headquartersId, String corpCode);

    /**
     * 협력사와 법인등록번호로 회사 정보를 검색합니다.
     *
     * @param partnerId 협력사 ID
     * @param corpCode  법인등록번호
     * @return 검색된 회사 정보 (Optional)
     */
    Optional<CompanyProfile> findByPartnerIdAndCorpCode(Long partnerId, String corpCode);


    /**
     * DART 기업 코드로 모든 회사 정보를 조회합니다. (중복 데이터 처리용)
     *
     * @param corpCode DART 기업 코드
     * @return 해당 기업 코드를 가진 모든 회사 정보 목록
     */
    List<CompanyProfile> findAllByCorpCode(String corpCode);
}