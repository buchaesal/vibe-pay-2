package vibe.api.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 주문서 조회 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class OrderFormResponse {
    private MemberInfo memberInfo;
    private List<CartItem> cartList;
    private Integer totalAmount;
    private List<String> availablePayments;

    /**
     * 회원 정보
     */
    @Getter
    @Setter
    public static class MemberInfo {
        private String name;
        private String phone;
        private String email;
        private Integer points;
    }

    /**
     * 장바구니 아이템
     */
    @Getter
    @Setter
    public static class CartItem {
        private Long cartId;
        private String productNo;
        private String productName;
        private Integer qty;
        private Integer price;
    }
}
