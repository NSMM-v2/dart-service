/**
 * @file DartCorpCode.java
 * @description DART에서 제공하는 기업 고유번호 및 관련 정보를 저장하는 엔티티 클래스입니다.
 *              각 필드는 DART API 응답의 corpCode.xml 파일의 항목과 매핑됩니다.
 *              - corpCode: DART에서 발급하는 고유한 회사 코드 (8자리)
 *              - corpName: 회사의 정식 명칭
 *              - stockCode: 주식 시장에서 사용하는 종목 코드 (6자리, 상장된 경우)
 *              - modifyDate: 정보 최종 수정일 (YYYYMMDD 형식)
 *              - corpCls: 시장 구분 (Y: 유가증권, K: 코스닥, N: 코넥스, E: 기타)
 *              - createdAt: 레코드 생성 시간
 *              - updatedAt: 레코드 마지막 업데이트 시간
 */
package com.nsmm.esg.dart_service.database.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "dart_corp_codes")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DartCorpCode {

    @Id
    @Column(name = "corp_code", length = 8, nullable = false, unique = true)
    private String corpCode; // 고유번호

    @Column(name = "corp_name", nullable = false)
    private String corpName; // 정식회사명

    @Column(name = "corp_eng_name", nullable = false)
    private String corpEngName; // 영문 회사명

    @Column(name = "stock_code", length = 6)
    private String stockCode; // 종목코드 (상장사만 존재)

    @Column(name = "modify_date", nullable = false)
    private String modifyDate; // 최종변경일자 (YYYYMMDD)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}