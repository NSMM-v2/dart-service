/**
 * PartnerCompany - 협력사 관리 엔티티 (최소한 버전)
 * 
 * 설계 원칙:
 * - 계층형 구조 제거 (Auth Service에서 관리)
 * - 단순한 회사-소유자 매핑만 유지
 * - CompanyProfile 참조로 회사 정보 활용
 * 
 * 주요 변경사항:
 * - 계층형 관련 컬럼 모두 제거
 * - contract_end_date 제거
 * - headquartersId, partnerId 선택적 컬럼 추가
 * - userType으로 본사/협력사 구분
 */
package com.nsmm.esg.dart_service.database.entity;

import com.nsmm.esg.dart_service.partner.model.PartnerCompanyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "partner_companies", indexes = {
        @Index(name = "idx_partner_corp_code", columnList = "corp_code"),
        @Index(name = "idx_partner_headquarters_id", columnList = "headquarters_id"),
        @Index(name = "idx_partner_partner_id", columnList = "partner_id"),
        @Index(name = "idx_partner_company_profile", columnList = "company_profile_id"),
        @Index(name = "idx_partner_user_type", columnList = "user_type")
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
    // 회사 정보 연관관계
    // ========================================================================

    @Column(name = "corp_code", length = 8, nullable = false)
    private String corpCode; // DART 기업 코드

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_profile_id")
    private CompanyProfile companyProfile;

    // ========================================================================
    // 소유자 정보 (본사 또는 협력사)
    // ========================================================================

    @Column(name = "headquarters_id")
    private Long headquartersId; // 본사가 등록한 경우에만 값 존재

    @Column(name = "partner_id")
    private Long partnerId; // 협력사가 등록한 경우에만 값 존재

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType; // HEADQUARTERS 또는 PARTNER

    // ========================================================================
    // 계약 및 상태 관리
    // ========================================================================

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate; // 계약 시작일

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

    // ========================================================================
    // 편의 메서드
    // ========================================================================

    /**
     * 본사가 등록한 협력사인지 확인
     */
    public boolean isHeadquartersOwned() {
        return "HEADQUARTERS".equals(userType) && headquartersId != null;
    }

    /**
     * 협력사가 등록한 하위 협력사인지 확인
     */
    public boolean isPartnerOwned() {
        return "PARTNER".equals(userType) && partnerId != null;
    }

    /**
     * 소유자 ID 반환 (userType에 따라)
     */
    public Long getOwnerId() {
        return isHeadquartersOwned() ? headquartersId : partnerId;
    }
}