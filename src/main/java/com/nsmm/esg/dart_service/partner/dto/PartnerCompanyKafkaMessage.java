
package com.nsmm.esg.dart_service.partner.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PartnerCompanyKafkaMessage {

  /**
   * 기업 고유 코드 (DART API 기준)
   */
  private String corpCode;

  /**
   * 이벤트 액션 타입
   * - partner_company_registered: 협력사 등록됨
   * - partner_company_updated: 협력사 정보 업데이트됨
   * - partner_company_restored: 협력사 복원됨
   */
  private String action;

  /**
   * 협력사 ID (선택적)
   */
  private String partnerCompanyId;

  /**
   * 본사 ID (선택적)
   */
  private Long headquartersId;

  /**
   * 이벤트 발생 시각 (ISO 8601 형식)
   */
  private String timestamp;
}