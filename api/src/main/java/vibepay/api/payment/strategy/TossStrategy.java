package vibepay.api.payment.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vibepay.api.dto.request.CreateOrderRequest;
import vibepay.api.entity.Payment;
import vibepay.api.entity.PaymentInterface;
import vibepay.api.pg.TossClient;
import vibepay.api.repository.PaymentInterfaceTrxMapper;

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
    private final PaymentInterfaceTrxMapper paymentInterfaceTrxMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Payment approve(String orderNo, CreateOrderRequest.PaymentInfo paymentInfo) {
        try {
            Map<String, Object> authResult = paymentInfo.getAuthResult();

            // 1. 토스 승인 요청
            Map<String, Object> approvalResult = tossClient.approve(authResult);

            // 2. PAYMENT_INTERFACE 로그 저장 (트랜잭션 분리)
            savePaymentInterface("APPROVAL", orderNo, authResult, approvalResult, "SUCCESS");

            // 3. Payment 객체 생성
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
            // 실패 로그 저장
            try {
                savePaymentInterface("APPROVAL", orderNo, paymentInfo.getAuthResult(), null, "FAIL");
            } catch (Exception ignored) {
            }
            throw e;
        }
    }

    @Override
    public void netCancel(Map<String, Object> authResult) {
        try {
            // 토스는 망취소 API 없음 → 일반 취소 재사용
            tossClient.netCancel(authResult);

            // 망취소 로그 저장
            savePaymentInterface("NET_CANCEL", null, authResult, null, "SUCCESS");

        } catch (Exception e) {
            log.error("토스 망취소 실패 (무시)", e);
            try {
                savePaymentInterface("NET_CANCEL", null, authResult, null, "FAIL");
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void refund(String tid, Integer cancelAmount, Integer remainAmount) {
        try {
            // 토스는 전체/부분 취소 API 동일
            tossClient.refund(tid, cancelAmount);

            log.info("토스 취소 완료: paymentKey={}, amount={}", tid, cancelAmount);

        } catch (Exception e) {
            log.error("토스 취소 실패", e);
            throw e;
        }
    }

    /**
     * PAYMENT_INTERFACE 로그 저장 (트랜잭션 분리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void savePaymentInterface(String transactionType, String orderNo,
                                        Map<String, Object> request,
                                        Map<String, Object> response,
                                        String result) {
        try {
            PaymentInterface paymentInterface = new PaymentInterface();
            paymentInterface.setPgType("TOSS");
            paymentInterface.setTransactionType(transactionType);
            paymentInterface.setOrderNo(orderNo);
            paymentInterface.setRequestJson(objectMapper.writeValueAsString(request));
            paymentInterface.setResponseJson(response != null ? objectMapper.writeValueAsString(response) : null);
            paymentInterface.setResult(result);
            paymentInterface.setTransactionDatetime(LocalDateTime.now());

            paymentInterfaceTrxMapper.insertPaymentInterface(paymentInterface);

        } catch (Exception e) {
            log.error("PAYMENT_INTERFACE 저장 실패 (무시)", e);
        }
    }
}
