package vibe.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 결제 정보 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-31
 */
@Getter
@Setter
public class PaymentInfo {
    private Long paymentNo;  // 결제번호 (채번 후 set)

    @NotBlank
    private String pgType;  // INICIS, TOSS

    @NotBlank
    private String method;  // CARD, POINT

    @NotNull
    private Integer amount;

    private Map<String, Object> authResult;  // PG 인증 응답 (이니시스/토스 구조 그대로)
}
