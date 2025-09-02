/**
 * @file FinancialStatementData.java
 * @description 기업의 재무제표 항목 데이터를 저장하는 엔티티 클래스입니다.
 *              DART API의 '단일회사 전체 재무제표' 조회 결과를 기반으로 하며, 각 레코드는 특정 회사의 특정 보고서의 한 재무 계정 항목을 나타냅니다.
 *              - id: 기본 키, 자동 생성
 *              - corpCode: 회사 고유번호 (DART에서 발급)
 *              - bsnsYear: 사업 연도 (YYYY 형식)
 *              - reprtCode: 보고서 코드 (예: 11011-사업보고서, 11012-반기보고서 등)
 *              - sjDiv: 재무제표 구분 (BS: 재무상태표, IS: 손익계산서, CF: 현금흐름표 등)
 *              - accountId: 계정 ID (XBRL 표준 계정 ID 또는 DART 자체 ID)
 *              - accountNm: 계정명 (예: 유동자산, 매출액)
 *              - thstrmNm: 당기 명칭 (예: 제 50 기)
 *              - thstrmAmount: 당기 금액 (숫자형 문자열)
 *              - thstrmAddAmount: 당기 누적 금액 (손익계산서 등 해당되는 경우)
 *              - frmtrmNm: 전기 명칭
 *              - frmtrmAmount: 전기 금액
 *              - frmtrmQNm: 전기 분기/반기 명칭
 *              - frmtrmQAmount: 전기 분기/반기 금액
 *              - frmtrmAddAmount: 전기 누적 금액
 *              - bfefrmtrmNm: 전전기 명칭
 *              - bfefrmtrmAmount: 전전기 금액
 *              - currency: 통화 단위 (예: KRW)
 *              - createdAt: 레코드 생성 시간
 *              - updatedAt: 레코드 마지막 업데이트 시간
 */
package com.nsmm.esg.dart_service.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "financial_statement_data", indexes = {
        @Index(name = "idx_fs_corp_year_reprt", columnList = "corpCode, bsnsYear, reprtCode")
})
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialStatementData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String corpCode; // 회사 고유번호

    @Column(nullable = false, length = 4)
    private String bsnsYear; // 사업 연도

    @Column(nullable = false, length = 5)
    private String reprtCode; // 보고서 코드

    @Column(length = 10)
    private String sjDiv; // 재무제표구분 (BS, IS, CF 등)

    @Column(length = 255)
    private String accountId; // 계정ID (XBRL 표준계정ID)

    @Column(nullable = false)
    private String accountNm; // 계정명

    @Column(length = 50)
    private String thstrmNm; // 당기명 (예: 제 13 기)

    @Column(length = 50)
    private String thstrmAmount; // 당기금액 (문자열로 저장, 필요시 BigDecimal 변환)

    @Column(length = 50)
    private String thstrmAddAmount; // 당기누적금액 (손익계산서)

    @Column(length = 50)
    private String frmtrmNm; // 전기명

    @Column(length = 50)
    private String frmtrmAmount; // 전기금액

    @Column(length = 50)
    private String frmtrmQNm; // 전기명(분/반기)

    @Column(length = 50)
    private String frmtrmQAmount; // 전기금액(분/반기)
    
    @Column(length = 50)
    private String frmtrmAddAmount; // 전기누적금액

    @Column(length = 50)
    private String bfefrmtrmNm; // 전전기명

    @Column(length = 50)
    private String bfefrmtrmAmount; // 전전기금액

    @Column(length = 10)
    private String currency; // 통화 단위

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
} 