/**
 * @file CompanyProfile.java
 * @description DART에 등록된 회사(기업)의 개황 정보를 저장하는 엔티티 클래스입니다.
 *              DART API의 '회사 개황 조회' 결과를 기반으로 하며, 각 레코드는 하나의 회사 프로필을 나타냅니다.
 *              - corpCode: DART에서 발급하는 고유한 회사 코드 (8자리, 기본 키)
 *              - corpName: 회사의 정식 명칭
 *              - corpNameEng: 회사의 영문 명칭
 *              - stockCode: 주식 시장에서 사용하는 종목 코드 (6자리, 상장된 경우)
 *              - stockName: 종목명 (예: "삼성전자")
 *              - ceoName: 대표이사 이름
 *              - corpClass: 시장 구분 (Y: 유가증권, K: 코스닥, N: 코넥스, E: 기타)
 *              - businessNumber: 사업자 등록번호 (하이픈 포함 가능)
 *              - corporateRegistrationNumber: 법인 등록번호 (하이픈 포함 가능)
 *              - address: 회사 주소
 *              - homepageUrl: 회사 홈페이지 URL
 *              - irUrl: IR(Investor Relations) 페이지 URL
 *              - phoneNumber: 회사 대표 전화번호
 *              - faxNumber: 회사 대표 팩스번호
 *              - industry: 업종명 (텍스트)
 *              - industryCode: 업종 코드 (숫자, 예: "264")
 *              - establishmentDate: 설립일 (YYYYMMDD 형식)
 *              - accountingMonth: 결산월 (MM 형식)
 *              - createdAt: 레코드 생성 시간
 *              - updatedAt: 레코드 마지막 업데이트 시간
 */
package com.nsmm.esg.dart_service.database.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "company_profiles")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfile {

    @Id
    @Column(name = "corp_code", length = 8)
    private String corpCode; // DART 고유번호

    @Column(name = "corp_name", nullable = false)
    private String corpName; // 정식 회사명

    @Column(name = "corp_name_eng")
    private String corpNameEng; // 영문 회사명

    @Column(name = "stock_code", length = 6)
    private String stockCode; // 종목 코드

    @Column(name = "stock_name", length = 100)
    private String stockName; // 종목명

    @Column(name = "ceo_name")
    private String ceoName; // 대표이사명

    @Column(name = "corp_class", length = 10)
    private String corpClass; // 시장 구분

    @Column(name = "business_number", length = 13)
    private String businessNumber; // 사업자등록번호

    @Column(name = "corporate_registration_number", length = 13)
    private String corporateRegistrationNumber; // 법인등록번호

    @Column(name = "address", length = 500)
    private String address; // 주소

    @Column(name = "homepage_url", length = 200)
    private String homepageUrl; // 홈페이지 URL

    @Column(name = "ir_url", length = 200)
    private String irUrl; // IR URL

    @Column(name = "phone_number", length = 20)
    private String phoneNumber; // 전화번호

    @Column(name = "fax_number", length = 20)
    private String faxNumber; // 팩스번호

    @Column(name = "industry", length = 200)
    private String industry; // 업종명 (텍스트)

    @Column(name = "industry_code", length = 10)
    private String industryCode; // 업종 코드 (숫자)

    @Column(name = "establishment_date", length = 8)
    private String establishmentDate; // 설립일 (YYYYMMDD)

    @Column(name = "accounting_month", length = 2)
    private String accountingMonth; // 결산월 (MM)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}