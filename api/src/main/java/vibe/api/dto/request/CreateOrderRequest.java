package vibe.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 주문 생성 요청 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class CreateOrderRequest {
    @NotBlank
    private String orderNo;

    @NotBlank
    private String memberNo;

    @NotBlank
    private String ordererName;

    @NotBlank
    private String ordererPhone;

    private String ordererEmail;

    @NotEmpty
    private List<PaymentInfo> payments;

    @NotEmpty
    private List<Long> cartIdList;

    /**
     * 결제 정보
     */
    @Getter
    @Setter
    public static class PaymentInfo {
        @NotBlank
        private String pgType;  // INICIS, TOSS

        @NotBlank
        private String method;  // CARD, POINT

        @NotNull
        private Integer amount;

        private Map<String, Object> authResult;  // PG 인증 응답 (이니시스/토스 구조 그대로)
    }
}
