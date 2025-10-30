package vibepay.api.service;

import vibepay.api.dto.request.CreateOrderRequest;
import vibepay.api.dto.request.OrderCancelRequest;
import vibepay.api.dto.response.CreateOrderResponse;
import vibepay.api.dto.response.OrderCompleteResponse;
import vibepay.api.dto.response.OrderDetailResponse;
import vibepay.api.dto.response.OrderFormResponse;
import vibepay.api.dto.response.OrderHistoryResponse;
import vibepay.api.dto.response.OrderSequenceResponse;

import java.util.List;

/**
 * 주문 서비스 인터페이스
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
public interface OrderService {
    /**
     * 주문서 데이터 조회
     */
    OrderFormResponse getOrderFormData(String memberNo);

    /**
     * 주문번호 채번
     */
    OrderSequenceResponse getNextOrderNo();

    /**
     * 주문 생성 (결제 포함)
     */
    CreateOrderResponse createOrder(CreateOrderRequest request);

    /**
     * 주문 내역 조회
     */
    List<OrderHistoryResponse> getOrderHistory(String memberNo);

    /**
     * 주문 완료 조회
     */
    OrderCompleteResponse getOrderComplete(String orderNo);

    /**
     * 주문 상세 조회
     */
    OrderDetailResponse getOrderDetail(String orderNo);

    /**
     * 주문 취소
     */
    void cancelOrder(OrderCancelRequest request);
}
