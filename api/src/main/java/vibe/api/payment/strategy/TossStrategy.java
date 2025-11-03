package vibe.api.payment.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vibe.api.pg.TossClient;

import java.util.Map;

/**
 * 토스 PG 전략
 *
 * @author Claude
 * @version 1.0
 * @since 2025-11-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossStrategy implements PgStrategy {

    private final TossClient tossClient;

    @Override
    public boolean supports(String pgType) {
        return "TOSS".equals(pgType);
    }

    @Override
    public Map<String, Object> approve(String orderNo, Long paymentNo, Map<String, Object> authResult) {
        try {
            // 토스 승인 요청 (로깅은 Client에서 처리)
            Map<String, Object> approvalResult = tossClient.approve(orderNo, paymentNo, authResult);
            log.info("토스 승인 완료: orderNo={}, paymentKey={}", orderNo, approvalResult.get("paymentKey"));
            return approvalResult;

        } catch (Exception e) {
            log.error("토스 승인 실패", e);
            throw e;
        }
    }

    @Override
    public void netCancel(String orderNo, Long paymentNo, Map<String, Object> authResult) {
        try {
            // 토스는 망취소 API 없음 → 일반 취소 재사용 (로깅은 Client에서 처리)
            tossClient.netCancel(orderNo, paymentNo, authResult);

        } catch (Exception e) {
            log.error("토스 망취소 실패 (무시)", e);
        }
    }

    @Override
    public void refund(String orderNo, Long paymentNo, String tid, Integer cancelAmount, Integer remainAmount, Integer originalAmount) {
        try {
            // 토스는 전체/부분 취소 API 동일 (로깅은 Client에서 처리)
            tossClient.refund(orderNo, paymentNo, tid, cancelAmount);

            // 전체 취소 vs 부분 취소 로그 구분
            boolean isFullCancel = originalAmount.equals(cancelAmount);
            log.info("토스 취소 완료: paymentKey={}, amount={}, type={}",
                tid, cancelAmount, isFullCancel ? "전체취소" : "부분취소");

        } catch (Exception e) {
            log.error("토스 취소 실패", e);
            throw e;
        }
    }
}
