package vibe.api.payment.strategy;

import vibe.api.dto.request.OrderInfo;
import vibe.api.dto.request.PaymentInfo;
import vibe.api.entity.Payment;
import vibe.api.payment.dto.ApprovalResult;

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
     * 각 전략이 승인 결과와 망취소 필요 여부를 반환합니다.
     * - 카드: PG 승인 후 needsNetCancel = true
     * - 적립금: DB UPDATE 후 needsNetCancel = false (롤백 가능)
     *
     * @param orderInfo 주문 정보
     * @param paymentInfo 결제 정보
     * @return 승인 결과 (Payment, 망취소 필요 여부, 망취소 컨텍스트)
     */
    ApprovalResult approve(OrderInfo orderInfo, PaymentInfo paymentInfo);

    /**
     * 망취소
     *
     * @param approvalResult 승인 결과 (망취소 컨텍스트 포함)
     */
    void netCancel(ApprovalResult approvalResult);

    /**
     * 취소 (전체 또는 부분)
     *
     * @param payment 원결제 정보
     * @param cancelAmount 취소금액
     * @param remainAmount 잔여금액
     */
    void refund(Payment payment, Integer cancelAmount, Integer remainAmount);
}