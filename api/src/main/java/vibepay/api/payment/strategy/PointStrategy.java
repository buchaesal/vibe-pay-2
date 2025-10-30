package vibepay.api.payment.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vibepay.api.common.enums.ErrorCode;
import vibepay.api.common.exception.ApiException;
import vibepay.api.dto.request.CreateOrderRequest;
import vibepay.api.entity.Member;
import vibepay.api.entity.Payment;
import vibepay.api.repository.MemberMapper;
import vibepay.api.repository.MemberTrxMapper;

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

    @Override
    public Payment approve(String orderNo, CreateOrderRequest.PaymentInfo paymentInfo) {
        // authResult에서 memberNo 추출 (Frontend에서 전달)
        String memberNo = (String) paymentInfo.getAuthResult().get("memberNo");

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
    public void refund(String tid, Integer cancelAmount, Integer remainAmount) {
        // TODO: Iteration 8에서 구현 (적립금 환불 = 회원에게 포인트 복구)
        log.info("적립금 환불: amount={}", cancelAmount);
    }
}
