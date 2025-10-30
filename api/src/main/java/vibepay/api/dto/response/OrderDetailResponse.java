package vibepay.api.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 주문 상세 조회 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class OrderDetailResponse {
    private String orderNo;
    private String orderDate;  // YYYY-MM-DD HH:mm 형식
    private OrdererInfo orderer;
    private List<OrderDetailItem> items;
    private List<PaymentInfo> payments;

    /**
     * 주문자 정보
     */
    @Getter
    @Setter
    public static class OrdererInfo {
        private String name;
        private String phone;
        private String email;
    }

    /**
     * 주문 상품 정보
     */
    @Getter
    @Setter
    public static class OrderDetailItem {
        private Integer orderSeq;
        private String productNo;
        private String productName;
        private Integer price;
        private Integer qty;
        private Integer cancelQty;
    }

    /**
     * 결제 정보
     */
    @Getter
    @Setter
    public static class PaymentInfo {
        private String method;  // CARD, POINT
        private Integer amount;
    }
}
