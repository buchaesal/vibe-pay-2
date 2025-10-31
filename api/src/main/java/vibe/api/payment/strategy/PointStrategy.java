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
 * 적립금 결제 전략
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointStrategy implements PaymentStrategy {

    private final MemberMapper memberMapper;
    private final MemberTrxMapper memberTrxMapper;
    private final OrderMapper orderMapper;

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
        payment.setPgType(null);  // 적립금은 PG 없음
        payment.setPaymentMethod("POINT");
        payment.setPaymentAmount(paymentInfo.getAmount());
        payment.setTid(null);
        payment.setApprovalNo(null);
        payment.setRemainRefundableAmount(paymentInfo.getAmount());
        payment.setPaymentDatetime(LocalDateTime.now());

        log.info("적립금 결제 완료: orderNo={}, memberNo={}, amount={}",
            orderNo, memberNo, paymentInfo.getAmount());

        return payment;
    }

    @Override
    public void netCancel(Map<String, Object> authResult) {
        // 적립금은 DB 롤백으로 처리되므로 불필요
        log.debug("적립금 망취소: DB 롤백으로 처리");
    }

    @Override
    public void refund(String orderNo, Long paymentNo, String tid, Integer cancelAmount, Integer remainAmount) {
        // 1. 주문번호로 회원번호 조회
        String memberNo = orderMapper.selectMemberNoByOrderNo(orderNo);
        if (memberNo == null) {
            throw new ApiException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 회원 포인트 복구 (환불금액만큼 증가)
        memberTrxMapper.updateMemberPoints(memberNo, cancelAmount);

        log.info("적립금 환불 완료: orderNo={}, memberNo={}, amount={}", orderNo, memberNo, cancelAmount);
    }
}
