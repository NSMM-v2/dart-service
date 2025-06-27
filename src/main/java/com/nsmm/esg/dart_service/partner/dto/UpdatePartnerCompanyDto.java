/**
 * @file UpdatePartnerCompanyDto.java
 * @description 기존 파트너사 정보 수정 요청 시 사용되는 DTO입니다.
 *              모든 필드는 선택적으로 제공될 수 있으며, 제공된 필드만 업데이트됩니다.
 *              `CreatePartnerCompanyDto`에서 일부 필드가 제거되었으나, 수정 DTO에서는 해당 필드들의 수정을 허용할 수 있습니다.
 *              (현재는 `CreatePartnerCompanyDto`와 유사하게 `contractEndDate`, `industry`, `country`, `address` 필드를 포함하고 있습니다.)
 */
package com.nsmm.esg.dart_service.partner.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.nsmm.esg.dart_service.partner.model.PartnerCompanyStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "파트너사 정보 업데이트 요청 시 사용되는 DTO. 모든 필드는 선택 사항입니다.")
public class UpdatePartnerCompanyDto {
    
    @Size(max = 255, message = "회사명은 최대 255자까지 입력 가능합니다.")
    @Schema(description = "변경할 파트너 회사명", example = "주식회사 뉴파트너", nullable = true)
    private String companyName;
    
    @Size(min = 8, max = 8, message = "DART 기업 고유 코드는 8자리여야 합니다.")
    @Schema(description = "변경할 DART 기업 고유 코드 (8자리 숫자). 변경 시 DART에서 추가 정보를 다시 조회합니다.", example = "00654321", nullable = true)
    private String corpCode;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "변경할 파트너사와의 계약 시작일 (YYYY-MM-DD 형식)", example = "2024-01-01", nullable = true)
    private LocalDate contractStartDate;
    
    @Schema(description = "변경할 파트너사 상태 (예: ACTIVE, INACTIVE). 상태 변경은 신중해야 합니다.", example = "ACTIVE", nullable = true)
    private PartnerCompanyStatus status;
    
    @Size(max = 10, message = "주식 코드는 최대 10자까지 입력 가능합니다.")
    @Schema(description = "변경할 주식 코드 (선택사항, DART API에서 자동으로 조회되지만 수동으로도 설정 가능)", example = "005930", nullable = true)
    private String stockCode;
    
    // 참고: DART API로부터 자동 업데이트되는 필드(예: stockCode, industry, address, country)는
    // 이 DTO를 통해 직접 수정하지 않습니다. corpCode 변경 시 해당 정보는 자동으로 갱신됩니다.
    // 만약 수동으로 해당 필드를 관리해야 한다면, 별도의 필드를 추가하고 서비스 로직을 수정해야 합니다.
}