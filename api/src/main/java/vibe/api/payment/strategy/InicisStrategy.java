package vibe.api.payment.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vibe.api.dto.request.OrderInfo;
import vibe.api.dto.request.PaymentInfo;
import vibe.api.entity.Payment;
import vibe.api.pg.InicisClient;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 이니시스 결제 전략
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InicisStrategy implements PaymentStrategy {

    private final InicisClient inicisClient;

    @Override
    public Payment approve(OrderInfo orderInfo, PaymentInfo paymentInfo) {
        try {
            String orderNo = orderInfo.getOrderNo();
            Long paymentNo = paymentInfo.getPaymentNo();
            Map<String, Object> authResult = paymentInfo.getAuthResult();

            // 1. 이니시스 승인 요청 (로깅은 Client에서 처리)
            Map<String, Object> approvalResult = inicisClient.approve(orderNo, paymentNo, authResult);

            // 2. Payment 객체 생성
            Payment payment = new Payment();
            payment.setOrderNo(orderNo);
            payment.setPaymentType("PAYMENT");
            payment.setPgType("INICIS");
            payment.setPaymentMethod("CARD");
            payment.setPaymentAmount(paymentInfo.getAmount());
            payment.setTid((String) approvalResult.get("tid"));
            payment.setApprovalNo((String) approvalResult.get("applNum"));
            payment.setRemainRefundableAmount(paymentInfo.getAmount());
            payment.setPaymentDatetime(LocalDateTime.now());

            log.info("이니시스 승인 완료: orderNo={}, tid={}", orderNo, payment.getTid());

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
            // 망취소 (로깅은 Client에서 처리)
            inicisClient.netCancel(orderNo, paymentNo, authResult);

        } catch (Exception e) {
            log.error("이니시스 망취소 실패 (무시)", e);
        }
    }

    @Override
    public void refund(String orderNo, Long paymentNo, String tid, Integer cancelAmount, Integer remainAmount) {
        try {
            // 전체 취소 vs 부분 취소 판단
            if (remainAmount == 0) {
                // 전체 취소 (로깅은 Client에서 처리)
                inicisClient.refund(orderNo, paymentNo, tid);
            } else {
                // 부분 취소 (로깅은 Client에서 처리)
                inicisClient.partialRefund(orderNo, paymentNo, tid, cancelAmount, remainAmount);
            }

            log.info("이니시스 취소 완료: tid={}, amount={}", tid, cancelAmount);

        } catch (Exception e) {
            log.error("이니시스 취소 실패", e);
            throw e;
        }
    }

}
