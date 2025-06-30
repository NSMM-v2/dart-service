/**
 * PartnerCompany - 협력사 관리 엔티티 (계층형 구조 + DART 연동)
 * 
 * 핵심 개선사항:
 * - 회사 정보는 CompanyProfile과 연관관계로 처리
 * - 중복 데이터 제거하고 DART API 데이터 활용
 * - 계층형 구조로 본사-협력사 관계 관리
 * - 게이트웨이 헤더 기반 권한 제어 (X-HEADQUARTERS-ID, X-PARTNER-ID)
 * 
 * 데이터 소스:
 * - CompanyProfile: 회사 기본 정보 (회사명, 주소, 대표자, 업종 등)
 * - DartCorpCode: 기업 코드 및 종목 코드
 * - FinancialStatementData: 재무 데이터 (필요시 조회)
 * - Disclosure: 공시 정보 (필요시 조회)
 */
package com.nsmm.esg.dart_service.database.entity;

import com.nsmm.esg.dart_service.partner.model.PartnerCompanyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "partner_companies", indexes = {
        @Index(name = "idx_partner_corp_code", columnList = "corp_code"),
        @Index(name = "idx_partner_headquarters_id", columnList = "headquarters_id"),
        @Index(name = "idx_partner_parent_id", columnList = "parent_partner_id"),
        @Index(name = "idx_partner_tree_path", columnList = "tree_path"),
        @Index(name = "idx_partner_hq_hierarchical", columnList = "hq_account_number,hierarchical_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerCompany {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String id;

    // ========================================================================
    // 회사 정보 연관관계 (DART API 데이터 활용)
    // ========================================================================

    /**
     * DART 회사 프로필과의 연관관계
     * 회사 기본 정보는 모두 CompanyProfile에서 조회
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corp_code", referencedColumnName = "corp_code")
    private CompanyProfile companyProfile;

    // ========================================================================
    // 본사/협력사 구분 및 관계 (게이트웨이 헤더와 매핑)
    // ========================================================================

    /**
     * 본사 ID - auth-service의 Headquarters ID와 매핑
     * 게이트웨이 헤더: X-HEADQUARTERS-ID
     */
    @Column(name = "headquarters_id", nullable = false)
    private Long headquartersId;

    /**
     * 협력사 등록한 사용자 ID - auth-service의 Partner ID와 매핑
     * 게이트웨이 헤더: X-PARTNER-ID (협력사인 경우에만)
     */
    @Column(name = "partner_id")
    private Long partnerId; // 협력사인 경우에만 존재, 본사가 등록한 경우 null

    @Column(name = "hq_account_number", nullable = false, length = 10)
    private String hqAccountNumber; // 본사 계정 번호 (예: HQ001)

    // ========================================================================
    // 계층형 구조 필드들 (auth-service Partner와 동일)
    // ========================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_partner_id")
    private PartnerCompany parentPartner; // 상위 협력사

    @OneToMany(mappedBy = "parentPartner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PartnerCompany> childPartners = new ArrayList<>(); // 하위 협력사 목록

    @Column(name = "hierarchical_id", nullable = false, length = 20)
    private String hierarchicalId; // 계층적 ID (L1-001, L2-001...)

    @Column(name = "level", nullable = false)
    private Integer level; // 협력사 계층 레벨 (1차=1, 2차=2...)

    @Column(name = "tree_path", nullable = false, length = 500)
    private String treePath; // 트리 경로 (/{본사ID}/L1-001/L2-001/...)

    // ========================================================================
    // 계약 및 상태 관리
    // ========================================================================

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate; // 계약 시작일

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate; // 계약 종료일

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PartnerCompanyStatus status = PartnerCompanyStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}