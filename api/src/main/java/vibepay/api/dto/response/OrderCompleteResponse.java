package vibepay.api.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 주문 완료 조회 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class OrderCompleteResponse {
    private String orderNo;
    private String orderDate;  // YYYY-MM-DD HH:mm 형식
    private Integer totalAmount;
    private String paymentStatus;  // SUCCESS
    private List<OrderCompleteItem> items;
    private List<PaymentInfo> payments;

    /**
     * 주문 상품 정보
     */
    @Getter
    @Setter
    public static class OrderCompleteItem {
        private String productName;
        private Integer price;
        private Integer qty;
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
