package vibe.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;
import vibe.api.dto.request.CreateOrderRequest;
import vibe.api.dto.request.OrderInfo;
import vibe.api.dto.request.PaymentInfo;
import vibe.api.dto.response.PaymentParamsResponse;
import vibe.api.entity.Payment;
import vibe.api.payment.strategy.PaymentMethodStrategy;
import vibe.api.payment.strategy.PgStrategy;
import vibe.api.pg.InicisClient;
import vibe.api.repository.PaymentMapper;
import vibe.api.repository.PaymentTrxMapper;
import vibe.api.service.PaymentService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 결제 서비스 구현
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final InicisClient inicisClient;
    private final List<PaymentMethodStrategy> paymentMethodStrategies;
    private final List<PgStrategy> pgStrategies;
    private final PaymentMapper paymentMapper;
    private final PaymentTrxMapper paymentTrxMapper;

    /**
     * 이니시스 인증 파라미터 생성
     */
    @Override
    public PaymentParamsResponse generateInicisAuthParams(String orderNo, Integer price) {
        log.debug("이니시스 인증 파라미터 생성: orderNo={}, price={}", orderNo, price);

        Map<String, String> params = inicisClient.generateAuthParams(orderNo, price);

        PaymentParamsResponse response = new PaymentParamsResponse();
        response.setMid(params.get("mid"));
        response.setTimestamp(params.get("timestamp"));
        response.setMKey(params.get("mKey"));
        response.setSignature(params.get("signature"));
        response.setVerification(params.get("verification"));

        return response;
    }

    /**
     * 결제 처리 (전략 패턴 + 망취소)
     */
    @Override
    @Transactional
    public void processPayments(CreateOrderRequest orderRequest) {
        String orderNo = orderRequest.getOrderInfo().getOrderNo();
        List<PaymentInfo> payments = orderRequest.getPayments();

        log.info("결제 처리 시작: orderNo={}, paymentCount={}", orderNo, payments.size());

        List<Map<String, Object>> authResults = new ArrayList<>();

        try {
            for (PaymentInfo paymentInfo : payments) {
                Map<String, Object> authResult = processIndividualPayment(orderRequest.getOrderInfo(), paymentInfo);
                authResults.add(authResult);
            }

            log.info("결제 처리 완료: orderNo={}, totalCount={}", orderNo, payments.size());

        } catch (Exception e) {
            log.error("결제 처리 실패: orderNo={}", orderNo, e);
            performNetCancellation(payments, authResults);
            throw new ApiException(ErrorCode.APPROVE_FAIL);
        }
    }

    /**
     * 개별 결제 처리
     */
    private Map<String, Object> processIndividualPayment(OrderInfo orderInfo, PaymentInfo paymentInfo) {
        Long paymentNo = paymentMapper.selectNextPaymentNo();
        paymentInfo.setPaymentNo(paymentNo);

        log.debug("결제 승인 시도: paymentNo={}, pgType={}, method={}, amount={}",
            paymentNo, paymentInfo.getPgType(), paymentInfo.getMethod(), paymentInfo.getAmount());

        PaymentMethodStrategy strategy = getPaymentMethodStrategy(paymentInfo.getMethod());
        Payment payment = strategy.approve(orderInfo, paymentInfo);

        payment.setPaymentNo(paymentNo);
        paymentTrxMapper.insertPayment(payment);

        log.info("결제 승인 및 저장 성공: paymentNo={}, method={}, amount={}",
            paymentNo, payment.getPaymentMethod(), payment.getPaymentAmount());

        return createAuthResultForNetCancel(orderInfo.getOrderNo(), paymentNo, paymentInfo);
    }

    /**
     * 망취소용 authResult 생성
     */
    private Map<String, Object> createAuthResultForNetCancel(String orderNo, Long paymentNo, PaymentInfo paymentInfo) {
        Map<String, Object> authResult = new HashMap<>(
            paymentInfo.getAuthResult() != null ? paymentInfo.getAuthResult() : new HashMap<>()
        );
        authResult.put("orderNo", orderNo);
        authResult.put("paymentNo", paymentNo);
        authResult.put("pgType", paymentInfo.getPgType());
        return authResult;
    }

    /**
     * 망취소 수행
     */
    private void performNetCancellation(List<PaymentInfo> payments, List<Map<String, Object>> authResults) {
        for (int i = 0; i < authResults.size(); i++) {
            try {
                PaymentInfo paymentInfo = payments.get(i);
                PaymentMethodStrategy strategy = getPaymentMethodStrategy(paymentInfo.getMethod());

                log.warn("망취소 시도: method={}", paymentInfo.getMethod());
                strategy.netCancel(authResults.get(i));

            } catch (Exception netCancelEx) {
                log.error("망취소 실패 (무시): method={}", payments.get(i).getMethod(), netCancelEx);
            }
        }
    }

    /**
     * 결제 수단 전략 찾기 (if 분기 없이)
     */

    private PaymentMethodStrategy getPaymentMethodStrategy(String method) {
        return paymentMethodStrategies.stream()
            .filter(strategy -> strategy.supports(method))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown payment method: " + method));
    }

    /**
     * PG 전략 찾기 (if 분기 없이)
     */
    private PgStrategy getPgStrategy(String pgType) {
        return pgStrategies.stream()
            .filter(strategy -> strategy.supports(pgType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown PG type: " + pgType));
    }

    /**
     * 결제 취소 처리
     *
     * 취소 금액 배분 로직:
     * 1. 카드 결제부터 우선 취소
     * 2. 카드로 취소 불가능한 잔액은 포인트로 취소
     */
    @Override
    @Transactional
    public List<Payment> processRefund(String orderNo, Integer cancelAmount) {
        log.info("결제 취소 시작: orderNo={}, cancelAmount={}", orderNo, cancelAmount);

        List<Payment> paymentList = getPaymentListSortedByPriority(orderNo);
        Integer remainAmount = cancelAmount;
        int refundCount = 0;

        for (Payment payment : paymentList) {
            if (remainAmount <= 0) break;

            int refundAmount = Math.min(remainAmount, payment.getRemainRefundableAmount());
            if (refundAmount <= 0) continue;

            processIndividualRefund(payment, refundAmount);
            refundCount++;
            remainAmount -= refundAmount;
        }

        validateRefundCompletion(remainAmount);
        log.info("결제 취소 완료: orderNo={}, refundCount={}", orderNo, refundCount);

        return new ArrayList<>();
    }

    /**
     * 환불 가능한 결제 목록 조회 (카드 우선 정렬)
     */
    private List<Payment> getPaymentListSortedByPriority(String orderNo) {
        List<Payment> paymentList = paymentMapper.selectPaymentListByOrderNo(orderNo);

        if (paymentList.isEmpty()) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        paymentList.sort((p1, p2) -> {
            boolean p1IsCard = "CARD".equals(p1.getPaymentMethod());
            boolean p2IsCard = "CARD".equals(p2.getPaymentMethod());
            return p1IsCard == p2IsCard ? 0 : (p1IsCard ? -1 : 1);
        });

        return paymentList;
    }

    /**
     * 개별 결제 환불 처리
     */
    private void processIndividualRefund(Payment payment, Integer refundAmount) {
        Integer newRemainAmount = payment.getRemainRefundableAmount() - refundAmount;

        log.debug("{} 취소: paymentNo={}, refundAmount={}, newRemainAmount={}",
            payment.getPaymentMethod(), payment.getPaymentNo(), refundAmount, newRemainAmount);

        // 결제 수단 전략으로 환불 처리 (카드는 내부에서 PG 전략 사용, 포인트는 직접 처리)
        PaymentMethodStrategy strategy = getPaymentMethodStrategy(payment.getPaymentMethod());
        strategy.refund(payment, refundAmount, newRemainAmount);

        // 환불 기록 저장
        Payment refundPayment = createRefundPayment(payment, refundAmount);
        paymentTrxMapper.insertPayment(refundPayment);

        // 원결제 환불가능금액 업데이트
        paymentTrxMapper.updateRemainRefundableAmount(payment.getPaymentNo(), newRemainAmount);
    }

    /**
     * 환불 Payment 객체 생성
     */
    private Payment createRefundPayment(Payment originalPayment, Integer refundAmount) {
        Payment refundPayment = new Payment();
        refundPayment.setOrderNo(originalPayment.getOrderNo());
        refundPayment.setPaymentType("REFUND");
        refundPayment.setPgType(originalPayment.getPgType());
        refundPayment.setPaymentMethod(originalPayment.getPaymentMethod());
        refundPayment.setPaymentAmount(refundAmount);
        refundPayment.setTid(originalPayment.getTid());
        refundPayment.setApprovalNo(originalPayment.getApprovalNo());
        refundPayment.setRemainRefundableAmount(0);
        refundPayment.setPaymentDatetime(LocalDateTime.now());
        return refundPayment;
    }

    /**
     * 환불 완료 검증
     */
    private void validateRefundCompletion(Integer remainAmount) {
        if (remainAmount > 0) {
            throw new ApiException(ErrorCode.CANCEL_FAIL);
        }
    }
}
