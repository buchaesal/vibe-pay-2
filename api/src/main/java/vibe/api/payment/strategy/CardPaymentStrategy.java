package vibe.api.payment.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vibe.api.dto.request.OrderInfo;
import vibe.api.dto.request.PaymentInfo;
import vibe.api.entity.Payment;
import vibe.api.payment.dto.ApprovalResult;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    public ApprovalResult approve(OrderInfo orderInfo, PaymentInfo paymentInfo) {
        String orderNo = orderInfo.getOrderNo();
        Long paymentNo = paymentInfo.getPaymentNo();
        String pgType = paymentInfo.getPgType();
        Map<String, Object> authResult = paymentInfo.getAuthResult();
        Map<String, Object> approvalResult = null;

        try {
            // PG 전략 찾기 (if 분기 없이)
            PgStrategy pgStrategy = findPgStrategy(pgType);

            // Step 1: PG 승인 요청 (외부 시스템 - 롤백 불가)
            approvalResult = pgStrategy.approve(orderNo, paymentNo, authResult);

            log.debug("PG 승인 성공 (여기서부터 에러 발생 시 망취소 필요): orderNo={}, pgType={}", orderNo, pgType);

            // 망취소용 컨텍스트 준비 (PG 승인 정보 포함)
            Map<String, Object> netCancelContext = new HashMap<>();
            netCancelContext.put("orderNo", orderNo);
            netCancelContext.put("paymentNo", paymentNo);
            netCancelContext.put("pgType", pgType);
            netCancelContext.put("authResult", authResult);
            netCancelContext.put("approvalResult", approvalResult);

            // Step 2: Payment 객체 생성 (여기서 에러 발생 가능)
            Payment payment = createPayment(orderNo, pgType, paymentInfo.getAmount(), approvalResult);

            log.info("카드 결제 승인 완료: orderNo={}, pgType={}, tid={}",
                orderNo, pgType, payment.getTid());

            // needsNetCancel = true (PG 승인 성공, 이후 에러 시 망취소 필요)
            return ApprovalResult.success(payment, true, netCancelContext);

        } catch (Exception e) {
            log.error("카드 결제 승인 실패: orderNo={}, pgType={}", orderNo, pgType, e);

            // PG 승인 성공 후 에러 발생 시
            if (approvalResult != null) {
                log.warn("PG 승인은 성공했으나 Payment 생성 중 에러 발생, 망취소 필요: orderNo={}", orderNo);

                Map<String, Object> netCancelContext = new HashMap<>();
                netCancelContext.put("orderNo", orderNo);
                netCancelContext.put("paymentNo", paymentNo);
                netCancelContext.put("pgType", pgType);
                netCancelContext.put("authResult", authResult);
                netCancelContext.put("approvalResult", approvalResult);

                return ApprovalResult.failed(true, netCancelContext);
            }

            // PG 승인 전 에러 (망취소 불필요)
            throw e;
        }
    }

    @Override
    public void netCancel(ApprovalResult approvalResult) {
        if (!approvalResult.isNeedsNetCancel()) {
            log.debug("망취소 불필요 (PG 승인 전 실패)");
            return;
        }

        Map<String, Object> netCancelContext = (Map<String, Object>) approvalResult.getNetCancelContext();
        if (netCancelContext == null) {
            log.warn("망취소 컨텍스트가 없어 망취소 불가");
            return;
        }

        String orderNo = (String) netCancelContext.get("orderNo");
        Long paymentNo = netCancelContext.get("paymentNo") != null
            ? ((Number) netCancelContext.get("paymentNo")).longValue()
            : null;
        String pgType = (String) netCancelContext.get("pgType");
        Map<String, Object> authResult = (Map<String, Object>) netCancelContext.get("authResult");
        Map<String, Object> approvalResultData = (Map<String, Object>) netCancelContext.get("approvalResult");

        try {
            log.info("카드 결제 망취소 시작: orderNo={}, pgType={}", orderNo, pgType);

            // authResult에 approvalResult 정보 병합 (망취소에 필요한 tid 등 포함)
            if (approvalResultData != null) {
                authResult.putAll(approvalResultData);
            }

            // PG 전략 찾기
            PgStrategy pgStrategy = findPgStrategy(pgType);

            // PG 망취소
            pgStrategy.netCancel(orderNo, paymentNo, authResult);

            log.info("카드 결제 망취소 완료: orderNo={}, pgType={}", orderNo, pgType);

        } catch (Exception e) {
            log.error("카드 결제 망취소 실패 (무시): orderNo={}, pgType={}", orderNo, pgType, e);
        }
    }

    @Override
    public void refund(Payment payment, Integer cancelAmount, Integer remainAmount) {
        try {
            String pgType = payment.getPgType();

            // PG 전략 찾기
            PgStrategy pgStrategy = findPgStrategy(pgType);

            // PG 환불 요청 (원결제금액 전달)
            pgStrategy.refund(payment.getOrderNo(), payment.getPaymentNo(), payment.getTid(),
                cancelAmount, remainAmount, payment.getPaymentAmount());

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