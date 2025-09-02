/**
 * @file PartnerCompanyStatus.java
 * @description 파트너 회사의 상태를 나타내는 열거형입니다.
 */
package com.nsmm.esg.dart_service.partner.model;

import lombok.Getter;

/**
 * 파트너 회사의 상태를 나타내는 열거형입니다.
 * ACTIVE: 활성 상태
 * INACTIVE: 비활성 상태
 */
@Getter
public enum PartnerCompanyStatus {
    ACTIVE("active"),
    INACTIVE("inactive");
    
    private final String value;
    
    PartnerCompanyStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}