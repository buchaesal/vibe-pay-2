package vibe.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    @NotNull
    private OrderInfo orderInfo;

    @NotEmpty
    private List<PaymentInfo> payments;
}
