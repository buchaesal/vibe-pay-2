package vibe.api.payment.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vibe.api.dto.request.OrderInfo;
import vibe.api.dto.request.PaymentInfo;
import vibe.api.entity.Payment;
import vibe.api.pg.TossClient;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 토스 결제 전략
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossStrategy implements PaymentStrategy {

    private final TossClient tossClient;

    @Override
    public Payment approve(OrderInfo orderInfo, PaymentInfo paymentInfo) {
        try {
            String orderNo = orderInfo.getOrderNo();
            Long paymentNo = paymentInfo.getPaymentNo();
            Map<String, Object> authResult = paymentInfo.getAuthResult();

            // 1. 토스 승인 요청 (로깅은 Client에서 처리)
            Map<String, Object> approvalResult = tossClient.approve(orderNo, paymentNo, authResult);

            // 2. Payment 객체 생성
            Payment payment = new Payment();
            payment.setOrderNo(orderNo);
            payment.setPaymentType("PAYMENT");
            payment.setPgType("TOSS");
            payment.setPaymentMethod("CARD");
            payment.setPaymentAmount(paymentInfo.getAmount());
            payment.setTid((String) approvalResult.get("paymentKey"));

            // card 객체에서 approveNo 추출
            Map<String, Object> card = (Map<String, Object>) approvalResult.get("card");
            if (card != null) {
                payment.setApprovalNo((String) card.get("approveNo"));
            }

            payment.setRemainRefundableAmount(paymentInfo.getAmount());
            payment.setPaymentDatetime(LocalDateTime.now());

            log.info("토스 승인 완료: orderNo={}, paymentKey={}", orderNo, payment.getTid());

            return payment;

        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void netCancel(Map<String, Object> authResult) {
        // authResult에서 orderNo, paymentNo 추출
        String orderNo = (String) authResult.get("orderNo");
        Long paymentNo = authResult.get("paymentNo") != null ? ((Number) authResult.get("paymentNo")).longValue() : null;

        try {
            // 토스는 망취소 API 없음 → 일반 취소 재사용 (로깅은 Client에서 처리)
            tossClient.netCancel(orderNo, paymentNo, authResult);

        } catch (Exception e) {
            log.error("토스 망취소 실패 (무시)", e);
        }
    }

    @Override
    public void refund(String orderNo, Long paymentNo, String tid, Integer cancelAmount, Integer remainAmount) {
        try {
            // 토스는 전체/부분 취소 API 동일 (로깅은 Client에서 처리)
            tossClient.refund(orderNo, paymentNo, tid, cancelAmount);

            log.info("토스 취소 완료: paymentKey={}, amount={}", tid, cancelAmount);

        } catch (Exception e) {
            log.error("토스 취소 실패", e);
            throw e;
        }
    }

}
