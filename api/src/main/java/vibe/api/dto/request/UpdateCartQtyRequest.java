package vibe.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 장바구니 수량 변경 요청 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-11-01
 */
@Data
public class UpdateCartQtyRequest {

    /**
     * 장바구니 ID
     */
    @NotNull(message = "장바구니 ID는 필수입니다")
    private Long cartId;

    /**
     * 변경할 수량
     */
    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    private Integer qty;
}