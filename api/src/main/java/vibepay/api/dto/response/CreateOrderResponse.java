package vibepay.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 주문 생성 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
@AllArgsConstructor
public class CreateOrderResponse {
    private String orderNo;
    private String paymentStatus;  // SUCCESS
}
