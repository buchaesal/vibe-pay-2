package vibe.api.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 주문 내역 조회 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class OrderHistoryResponse {
    private String orderNo;
    private String orderDate;  // YYYY-MM-DD 형식
    private Integer totalAmount;
    private String status;  // COMPLETE, CANCEL 등
    private List<OrderHistoryItem> items;

    /**
     * 주문 상품 정보
     */
    @Getter
    @Setter
    public static class OrderHistoryItem {
        private Integer orderSeq;
        private String productNo;
        private String productName;
        private Integer price;
        private Integer qty;
        private Integer cancelQty;
    }
}
