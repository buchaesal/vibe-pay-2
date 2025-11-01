package vibe.api.payment.strategy;

import vibe.api.dto.request.OrderInfo;
import vibe.api.dto.request.PaymentInfo;
import vibe.api.entity.Payment;

import java.util.Map;

/**
 * 결제 수단 전략 인터페이스
 *
 * 결제 수단(CARD, POINT)별로 전략을 구현합니다.
 *
 * @author Claude
 * @version 1.0
 * @since 2025-11-01
 */
public interface PaymentMethodStrategy {

    /**
     * 이 전략이 해당 결제수단을 지원하는지 확인
     *
     * @param method 결제수단 (CARD, POINT 등)
     * @return 지원 여부
     */
    boolean supports(String method);

    /**
     * 결제 승인
     *
     * @param orderInfo 주문 정보
     * @param paymentInfo 결제 정보
     * @return 결제 결과
     */
    Payment approve(OrderInfo orderInfo, PaymentInfo paymentInfo);

    /**
     * 망취소
     *
     * @param authResult 인증 결과
     */
    void netCancel(Map<String, Object> authResult);

    /**
     * 취소 (전체 또는 부분)
     *
     * @param payment 원결제 정보
     * @param cancelAmount 취소금액
     * @param remainAmount 잔여금액
     */
    void refund(Payment payment, Integer cancelAmount, Integer remainAmount);
}