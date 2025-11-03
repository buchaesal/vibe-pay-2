package vibe.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 장바구니 담기 요청 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class AddToCartRequest {
    @NotBlank(message = "회원번호는 필수입니다")
    private String memberNo;

    @NotEmpty(message = "상품 목록은 필수입니다")
    @Valid
    private List<CartItemRequest> items;

    /**
     * 장바구니 아이템 (상품번호 + 수량)
     */
    @Getter
    @Setter
    public static class CartItemRequest {
        @NotBlank(message = "상품번호는 필수입니다")
        private String productNo;

        @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
        private Integer qty;
    }
}
