package vibe.api.payment.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vibe.api.dto.request.OrderInfo;
import vibe.api.dto.request.PaymentInfo;
import vibe.api.entity.Payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 카드 결제 수단 전략
 *
 * PG사 전략을 사용하여 카드 결제를 처리합니다.
 *
 * @author Claude
 * @version 1.0
 * @since 2025-11-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardPaymentStrategy implements PaymentMethodStrategy {

    private final List<PgStrategy> pgStrategies;

    @Override
    public boolean supports(String method) {
        return "CARD".equals(method);
    }

    @Override
    public Payment approve(OrderInfo orderInfo, PaymentInfo paymentInfo) {
        try {
            String orderNo = orderInfo.getOrderNo();
            Long paymentNo = paymentInfo.getPaymentNo();
            String pgType = paymentInfo.getPgType();
            Map<String, Object> authResult = paymentInfo.getAuthResult();

            // PG 전략 찾기 (if 분기 없이)
            PgStrategy pgStrategy = findPgStrategy(pgType);

            // PG 승인 요청
            Map<String, Object> approvalResult = pgStrategy.approve(orderNo, paymentNo, authResult);

            // Payment 객체 생성
            Payment payment = createPayment(orderNo, pgType, paymentInfo.getAmount(), approvalResult);

            log.info("카드 결제 승인 완료: orderNo={}, pgType={}, tid={}",
                orderNo, pgType, payment.getTid());

            return payment;

        } catch (Exception e) {
            log.error("카드 결제 승인 실패", e);
            throw e;
        }
    }

    @Override
    public void netCancel(Map<String, Object> authResult) {
        String orderNo = (String) authResult.get("orderNo");
        Long paymentNo = authResult.get("paymentNo") != null
            ? ((Number) authResult.get("paymentNo")).longValue()
            : null;
        String pgType = (String) authResult.get("pgType");

        try {
            // PG 전략 찾기
            PgStrategy pgStrategy = findPgStrategy(pgType);

            // PG 망취소
            pgStrategy.netCancel(orderNo, paymentNo, authResult);

        } catch (Exception e) {
            log.error("카드 결제 망취소 실패 (무시): pgType={}", pgType, e);
        }
    }

    @Override
    public void refund(Payment payment, Integer cancelAmount, Integer remainAmount) {
        try {
            String pgType = payment.getPgType();

            // PG 전략 찾기
            PgStrategy pgStrategy = findPgStrategy(pgType);

            // PG 환불 요청
            pgStrategy.refund(payment.getOrderNo(), payment.getPaymentNo(), payment.getTid(),
                cancelAmount, remainAmount);

            log.info("카드 결제 환불 완료: orderNo={}, pgType={}, tid={}, amount={}",
                payment.getOrderNo(), pgType, payment.getTid(), cancelAmount);

        } catch (Exception e) {
            log.error("카드 결제 환불 실패", e);
            throw e;
        }
    }

    /**
     * PG 전략 찾기 (if 분기 없이)
     */
    private PgStrategy findPgStrategy(String pgType) {
        return pgStrategies.stream()
            .filter(strategy -> strategy.supports(pgType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown PG type: " + pgType));
    }

    /**
     * Payment 객체 생성
     */
    private Payment createPayment(String orderNo, String pgType, Integer amount, Map<String, Object> approvalResult) {
        Payment payment = new Payment();
        payment.setOrderNo(orderNo);
        payment.setPaymentType("PAYMENT");
        payment.setPgType(pgType);
        payment.setPaymentMethod("CARD");
        payment.setPaymentAmount(amount);
        payment.setRemainRefundableAmount(amount);
        payment.setPaymentDatetime(LocalDateTime.now());

        // PG별 응답 구조에 맞게 tid, approvalNo 추출
        if ("INICIS".equals(pgType)) {
            payment.setTid((String) approvalResult.get("tid"));
            payment.setApprovalNo((String) approvalResult.get("applNum"));
        } else if ("TOSS".equals(pgType)) {
            payment.setTid((String) approvalResult.get("paymentKey"));
            Map<String, Object> card = (Map<String, Object>) approvalResult.get("card");
            if (card != null) {
                payment.setApprovalNo((String) card.get("approveNo"));
            }
        }

        return payment;
    }
}