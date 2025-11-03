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
import vibe.api.payment.dto.ApprovalResult;
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
     *
     * ApprovalResult 기반으로 안전한 망취소 처리:
     * 1. 각 전략의 approve() 호출 즉시 결과를 리스트에 추가
     * 2. Payment INSERT 중 에러 발생해도 망취소 대상 확보
     * 3. needsNetCancel 플래그로 망취소 필요 여부 판단
     */
    @Override
    @Transactional
    public void processPayments(CreateOrderRequest orderRequest) {
        String orderNo = orderRequest.getOrderInfo().getOrderNo();
        List<PaymentInfo> payments = orderRequest.getPayments();

        log.info("결제 처리 시작: orderNo={}, paymentCount={}", orderNo, payments.size());

        List<ApprovalResult> approvalResults = new ArrayList<>();

        try {
            for (PaymentInfo paymentInfo : payments) {
                // approvalResults 리스트를 전달하여 내부에서 즉시 추가
                ApprovalResult result = processIndividualPayment(
                    orderRequest.getOrderInfo(),
                    paymentInfo,
                    approvalResults
                );

                // 승인 실패한 경우 (PG 승인은 성공했으나 Payment 생성 실패)
                if (!result.isSuccess()) {
                    throw new ApiException(ErrorCode.APPROVE_FAIL);
                }
            }

            log.info("결제 처리 완료: orderNo={}, totalCount={}", orderNo, payments.size());

        } catch (Exception e) {
            log.error("결제 처리 실패: orderNo={}", orderNo, e);
            performNetCancellation(approvalResults);
            throw new ApiException(ErrorCode.APPROVE_FAIL);
        }
    }

    /**
     * 개별 결제 처리
     *
     * approvalResults 리스트를 전달받아 approve() 성공 즉시 추가
     * → Payment INSERT 중 에러 발생해도 망취소 대상 확보
     */
    private ApprovalResult processIndividualPayment(
            OrderInfo orderInfo,
            PaymentInfo paymentInfo,
            List<ApprovalResult> approvalResults) {

        Long paymentNo = paymentMapper.selectNextPaymentNo();
        paymentInfo.setPaymentNo(paymentNo);

        log.debug("결제 승인 시도: paymentNo={}, pgType={}, method={}, amount={}",
            paymentNo, paymentInfo.getPgType(), paymentInfo.getMethod(), paymentInfo.getAmount());

        PaymentMethodStrategy strategy = getPaymentMethodStrategy(paymentInfo.getMethod());

        // 전략의 approve() 호출 (여기서 ApprovalResult 반환)
        ApprovalResult approvalResult = strategy.approve(orderInfo, paymentInfo);

        // approve() 성공 즉시 리스트에 추가! (이후 에러 발생해도 망취소 가능)
        approvalResults.add(approvalResult);
        log.debug("ApprovalResult 리스트에 추가 완료: needsNetCancel={}", approvalResult.isNeedsNetCancel());

        // 승인 성공한 경우에만 Payment INSERT (여기서 에러 나도 위에서 이미 리스트 추가함)
        if (approvalResult.isSuccess()) {
            // 망취소 테스트
//            if (true) {
//                throw new IllegalArgumentException();
//            }
            Payment payment = approvalResult.getPayment();
            payment.setPaymentNo(paymentNo);
            paymentTrxMapper.insertPayment(payment);

            log.info("결제 승인 및 저장 성공: paymentNo={}, method={}, amount={}",
                paymentNo, payment.getPaymentMethod(), payment.getPaymentAmount());
        } else {
            log.warn("결제 승인 실패 (PG 승인은 성공): paymentNo={}, needsNetCancel={}",
                paymentNo, approvalResult.isNeedsNetCancel());
        }

        return approvalResult;
    }

    /**
     * 망취소 수행
     *
     * needsNetCancel = true인 것만 망취소 처리
     * - 카드: true (PG 망취소 필요)
     * - 적립금: false (DB 롤백으로 자동 복구)
     */
    private void performNetCancellation(List<ApprovalResult> approvalResults) {
        for (ApprovalResult result : approvalResults) {
            if (!result.isNeedsNetCancel()) {
                log.debug("망취소 불필요 (DB 롤백으로 자동 복구)");
                continue;
            }

            try {
                Payment payment = result.getPayment();
                String method = payment != null ? payment.getPaymentMethod() : "UNKNOWN";
                PaymentMethodStrategy strategy = getPaymentMethodStrategy(method);

                log.warn("망취소 시도: method={}", method);
                strategy.netCancel(result);

            } catch (Exception netCancelEx) {
                log.error("망취소 실패 (무시)", netCancelEx);
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
