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

    @Column(name = "account_created", nullable = false)
    @Builder.Default
    private Boolean accountCreated = false; // 계정 생성 여부

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 계정 생성 상태를 업데이트
    public void setAccountCreated(boolean accountCreated) {
        this.accountCreated = accountCreated;
    }
}