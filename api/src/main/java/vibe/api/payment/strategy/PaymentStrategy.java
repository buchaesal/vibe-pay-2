package vibe.api.payment.strategy;

import vibe.api.dto.request.OrderInfo;
import vibe.api.dto.request.PaymentInfo;
import vibe.api.entity.Payment;

import java.util.Map;

/**
 * 결제 전략 인터페이스
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
public interface PaymentStrategy {
    /**
     * 결제 승인
     */
    Payment approve(OrderInfo orderInfo, PaymentInfo paymentInfo);

    /**
     * 망취소
     */
    void netCancel(Map<String, Object> authResult);

    /**
     * 취소 (전체 또는 부분)
     */
    void refund(String orderNo, Long paymentNo, String tid, Integer cancelAmount, Integer remainAmount);
}
