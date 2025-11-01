package vibe.api.payment.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;
import vibe.api.dto.request.OrderInfo;
import vibe.api.dto.request.PaymentInfo;
import vibe.api.entity.Member;
import vibe.api.entity.Payment;
import vibe.api.repository.MemberMapper;
import vibe.api.repository.MemberTrxMapper;
import vibe.api.repository.OrderMapper;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 포인트 결제 수단 전략
 *
 * @author Claude
 * @version 1.0
 * @since 2025-11-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointPaymentStrategy implements PaymentMethodStrategy {

    private final MemberMapper memberMapper;
    private final MemberTrxMapper memberTrxMapper;
    private final OrderMapper orderMapper;

    @Override
    public boolean supports(String method) {
        return "POINT".equals(method);
    }

    @Override
    public Payment approve(OrderInfo orderInfo, PaymentInfo paymentInfo) {
        String orderNo = orderInfo.getOrderNo();
        String memberNo = orderInfo.getMemberNo();

        // 1. 회원 적립금 조회
        Member member = memberMapper.selectMemberByMemberNo(memberNo)
            .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. 적립금 부족 확인
        if (member.getPoints() < paymentInfo.getAmount()) {
            throw new ApiException(ErrorCode.APPROVE_FAIL);
        }

        // 3. 적립금 차감
        memberTrxMapper.updateMemberPoints(memberNo, -paymentInfo.getAmount());

        // 4. Payment 객체 생성 (PG 없음)
        Payment payment = new Payment();
        payment.setOrderNo(orderNo);
        payment.setPaymentType("PAYMENT");
        payment.setPgType("POINT");  // 포인트는 자체 결제 수단
        payment.setPaymentMethod("POINT");
        payment.setPaymentAmount(paymentInfo.getAmount());
        payment.setTid(null);
        payment.setApprovalNo(null);
        payment.setRemainRefundableAmount(paymentInfo.getAmount());
        payment.setPaymentDatetime(LocalDateTime.now());

        log.info("포인트 결제 완료: orderNo={}, memberNo={}, amount={}",
            orderNo, memberNo, paymentInfo.getAmount());

        return payment;
    }

    @Override
    public void netCancel(Map<String, Object> authResult) {
        // 포인트는 DB 롤백으로 처리되므로 불필요
        log.debug("포인트 망취소: DB 롤백으로 처리");
    }

    @Override
    public void refund(Payment payment, Integer cancelAmount, Integer remainAmount) {
        // 1. 주문번호로 회원번호 조회
        String memberNo = orderMapper.selectMemberNoByOrderNo(payment.getOrderNo());
        if (memberNo == null) {
            throw new ApiException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 회원 포인트 복구 (환불금액만큼 증가)
        memberTrxMapper.updateMemberPoints(memberNo, cancelAmount);

        log.info("포인트 환불 완료: orderNo={}, memberNo={}, amount={}",
            payment.getOrderNo(), memberNo, cancelAmount);
    }
}
