package vibepay.api.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 장바구니 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class CartResponse {
    private Long cartId;
    private String productNo;
    private String productName;
    private Integer qty;
    private Integer price;
}
