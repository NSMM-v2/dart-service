/**
 * Kafka 메시지 전송용 DTO
 * Partner Company 관련 이벤트 메시지 구조 정의
 * 
 * 사용 목적:
 * - Producer와 Consumer 간 타입 안전성 보장
 * - 메시지 구조 명확화
 * - Jackson 직렬화/역직렬화 지원
 * 
 * @author ESG Project Team
 * @since 2024
 */
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
   * - partner_company_registered: 파트너사 등록됨
   * - partner_company_updated: 파트너사 정보 업데이트됨
   * - partner_company_restored: 파트너사 복원됨
   */
  private String action;

  /**
   * 파트너사 ID (선택적)
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