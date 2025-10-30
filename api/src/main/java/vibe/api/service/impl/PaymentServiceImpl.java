package vibe.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;
import vibe.api.dto.request.CreateOrderRequest;
import vibe.api.dto.response.PaymentParamsResponse;
import vibe.api.entity.Payment;
import vibe.api.payment.strategy.InicisStrategy;
import vibe.api.payment.strategy.PaymentStrategy;
import vibe.api.payment.strategy.PointStrategy;
import vibe.api.payment.strategy.TossStrategy;
import vibe.api.pg.InicisClient;
import vibe.api.repository.PaymentMapper;
import vibe.api.repository.PaymentTrxMapper;
import vibe.api.service.PaymentService;

import java.util.ArrayList;
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
    private final InicisStrategy inicisStrategy;
    private final TossStrategy tossStrategy;
    private final PointStrategy pointStrategy;
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
    public void processPayments(String orderNo, List<CreateOrderRequest.PaymentInfo> payments) {
        log.info("결제 처리 시작: orderNo={}, paymentCount={}", orderNo, payments.size());

        List<Payment> successPayments = new ArrayList<>();
        List<Map<String, Object>> authResults = new ArrayList<>();

        try {
            // 1. 각 결제수단 반복 처리
            for (CreateOrderRequest.PaymentInfo paymentInfo : payments) {
                log.debug("결제 승인 시도: pgType={}, method={}, amount={}",
                    paymentInfo.getPgType(), paymentInfo.getMethod(), paymentInfo.getAmount());

                // 2. 전략 선택
                PaymentStrategy strategy = getPaymentStrategy(paymentInfo);

                // 3. 승인 요청
                Payment payment = strategy.approve(orderNo, paymentInfo);

                successPayments.add(payment);
                authResults.add(paymentInfo.getAuthResult());

                log.info("결제 승인 성공: method={}, amount={}", payment.getPaymentMethod(), payment.getPaymentAmount());
            }

            // 4. 모든 결제 승인 성공 → PAYMENT INSERT
            for (Payment payment : successPayments) {
                paymentTrxMapper.insertPayment(payment);
            }

            log.info("결제 처리 완료: orderNo={}, totalCount={}", orderNo, successPayments.size());

        } catch (Exception e) {
            log.error("결제 처리 실패: orderNo={}", orderNo, e);

            // 5. 하나라도 실패 시 → 이전 승인건 망취소
            for (int i = 0; i < successPayments.size(); i++) {
                try {
                    CreateOrderRequest.PaymentInfo paymentInfo = payments.get(i);
                    PaymentStrategy strategy = getPaymentStrategy(paymentInfo);

                    log.warn("망취소 시도: index={}, method={}", i, paymentInfo.getMethod());
                    strategy.netCancel(authResults.get(i));

                } catch (Exception netCancelEx) {
                    log.error("망취소 실패 (무시): index={}", i, netCancelEx);
                }
            }

            throw new ApiException(ErrorCode.APPROVE_FAIL);
        }
    }

    /**
     * 전략 선택
     */
    private PaymentStrategy getPaymentStrategy(CreateOrderRequest.PaymentInfo paymentInfo) {
        if ("POINT".equals(paymentInfo.getMethod())) {
            return pointStrategy;
        } else if ("INICIS".equals(paymentInfo.getPgType())) {
            return inicisStrategy;
        } else if ("TOSS".equals(paymentInfo.getPgType())) {
            return tossStrategy;
        }

        throw new IllegalArgumentException("Unknown payment method: " + paymentInfo.getMethod());
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

        // 1. 환불 가능한 결제 목록 조회
        List<Payment> paymentList = paymentMapper.selectPaymentListByOrderNo(orderNo);

        if (paymentList.isEmpty()) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        List<Payment> refundPayments = new ArrayList<>();
        Integer remainCancelAmount = cancelAmount;

        // 2. 카드 결제부터 우선 취소
        for (Payment payment : paymentList) {
            if (!"CARD".equals(payment.getPaymentMethod())) {
                continue;
            }

            if (remainCancelAmount <= 0) {
                break;
            }

            Integer refundableAmount = payment.getRemainRefundableAmount();
            Integer refundAmount = Math.min(remainCancelAmount, refundableAmount);
            Integer newRemainAmount = refundableAmount - refundAmount;

            log.debug("카드 취소: tid={}, refundAmount={}, newRemainAmount={}",
                payment.getTid(), refundAmount, newRemainAmount);

            // PG 취소 API 호출
            PaymentStrategy strategy = getPaymentStrategyByPgType(payment.getPgType());
            strategy.refund(payment.getTid(), refundAmount, newRemainAmount);

            // 취소 결제 정보 생성
            Payment refundPayment = new Payment();
            refundPayment.setOrderNo(orderNo);
            refundPayment.setPaymentType("REFUND");
            refundPayment.setPgType(payment.getPgType());
            refundPayment.setPaymentMethod(payment.getPaymentMethod());
            refundPayment.setPaymentAmount(refundAmount);
            refundPayment.setTid(payment.getTid());
            refundPayment.setApprovalNo(payment.getApprovalNo());
            refundPayment.setRemainRefundableAmount(0);

            refundPayments.add(refundPayment);
            remainCancelAmount -= refundAmount;

            // 원결제의 환불가능금액 업데이트
            paymentTrxMapper.updateRemainRefundableAmount(payment.getPaymentNo(), newRemainAmount);
        }

        // 3. 포인트 취소
        for (Payment payment : paymentList) {
            if (!"POINT".equals(payment.getPaymentMethod())) {
                continue;
            }

            if (remainCancelAmount <= 0) {
                break;
            }

            Integer refundableAmount = payment.getRemainRefundableAmount();
            Integer refundAmount = Math.min(remainCancelAmount, refundableAmount);
            Integer newRemainAmount = refundableAmount - refundAmount;

            log.debug("포인트 취소: refundAmount={}", refundAmount);

            // 포인트 환불 (회원 포인트 증가는 OrderService에서 처리)
            Payment refundPayment = new Payment();
            refundPayment.setOrderNo(orderNo);
            refundPayment.setPaymentType("REFUND");
            refundPayment.setPgType("POINT");
            refundPayment.setPaymentMethod("POINT");
            refundPayment.setPaymentAmount(refundAmount);
            refundPayment.setTid(null);
            refundPayment.setApprovalNo(null);
            refundPayment.setRemainRefundableAmount(0);

            refundPayments.add(refundPayment);
            remainCancelAmount -= refundAmount;

            // 원결제의 환불가능금액 업데이트
            paymentTrxMapper.updateRemainRefundableAmount(payment.getPaymentNo(), newRemainAmount);
        }

        // 4. 취소 금액이 남아있으면 에러
        if (remainCancelAmount > 0) {
            throw new ApiException(ErrorCode.CANCEL_FAIL);
        }

        // 5. 취소 결제 정보 INSERT
        for (Payment refundPayment : refundPayments) {
            paymentTrxMapper.insertPayment(refundPayment);
        }

        log.info("결제 취소 완료: orderNo={}, refundCount={}", orderNo, refundPayments.size());

        return refundPayments;
    }

    /**
     * PG 타입으로 전략 선택
     */
    private PaymentStrategy getPaymentStrategyByPgType(String pgType) {
        if ("INICIS".equals(pgType)) {
            return inicisStrategy;
        } else if ("TOSS".equals(pgType)) {
            return tossStrategy;
        } else if ("POINT".equals(pgType)) {
            return pointStrategy;
        }

        throw new IllegalArgumentException("Unknown PG type: " + pgType);
    }
}
