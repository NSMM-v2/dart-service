/**
 * @file PartnerCompany.java
 * @description 서비스 사용자와 계약된 파트너 회사 정보를 저장하는 엔티티 클래스입니다.
 *              파트너사의 기본 정보, 계약 정보 및 상태를 관리합니다.
 *              - id: 기본 키, UUID로 자동 생성
 *              - memberId: 파트너사를 등록한 내부 사용자(회원)의 ID
 *              - companyName: 파트너 회사명
 *              - corpCode: 파트너 회사의 DART 고유번호 (8자리, 있는 경우)
 *              - stockCode: 파트너 회사의 종목 코드 (6자리, 상장된 경우)
 *              - contractStartDate: 계약 시작일
 *              - contractEndDate: 계약 종료일 (파트너사 삭제 요청 시 이 필드가 업데이트됨)
 *              - industry: 업종 (DTO에서 제거되었으나, 엔티티에는 유지될 수 있음 - 현재는 DTO와 동기화됨)
 *              - country: 국가 (DTO에서 제거되었으나, 엔티티에는 유지될 수 있음 - 현재는 DTO와 동기화됨)
 *              - address: 주소 (DTO에서 제거되었으나, 엔티티에는 유지될 수 있음 - 현재는 DTO와 동기화됨)
 *              - status: 파트너사 상태 (예: ACTIVE, INACTIVE, PENDING)
 *              - createdAt: 레코드 생성 시간
 *              - updatedAt: 레코드 마지막 업데이트 시간
 */
package com.example.javaversion.database.entity;

import com.example.javaversion.partner.model.PartnerCompanyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "partner_companies",
        indexes = {
                @Index(name = "idx_partner_companies_corp_code", columnList = "corp_code"),
                @Index(name = "idx_partner_companies_stock_code", columnList = "stock_code"),
                @Index(name = "idx_partner_companies_member_id", columnList = "member_id")
        }
)
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerCompany {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "CHAR(36) CHARACTER SET ascii")
    private String id;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "corp_code", length = 8)
    private String corpCode;

    @Column(name = "stock_code", length = 6)
    private String stockCode;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PartnerCompanyStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PartnerCompanyStatus.ACTIVE;
        }
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
