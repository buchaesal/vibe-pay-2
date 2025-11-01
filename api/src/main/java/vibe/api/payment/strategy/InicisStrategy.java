package vibe.api.payment.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vibe.api.pg.InicisClient;

import java.util.Map;

/**
 * 이니시스 PG 전략
 *
 * @author Claude
 * @version 1.0
 * @since 2025-11-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InicisStrategy implements PgStrategy {

    private final InicisClient inicisClient;

    @Override
    public boolean supports(String pgType) {
        return "INICIS".equals(pgType);
    }

    @Override
    public Map<String, Object> approve(String orderNo, Long paymentNo, Map<String, Object> authResult) {
        try {
            // 이니시스 승인 요청 (로깅은 Client에서 처리)
            Map<String, Object> approvalResult = inicisClient.approve(orderNo, paymentNo, authResult);
            log.info("이니시스 승인 완료: orderNo={}, tid={}", orderNo, approvalResult.get("tid"));
            return approvalResult;

        } catch (Exception e) {
            log.error("이니시스 승인 실패", e);
            throw e;
        }
    }

    @Override
    public void netCancel(String orderNo, Long paymentNo, Map<String, Object> authResult) {
        try {
            // 망취소 (로깅은 Client에서 처리)
            inicisClient.netCancel(orderNo, paymentNo, authResult);

        } catch (Exception e) {
            log.error("이니시스 망취소 실패 (무시)", e);
        }
    }

    @Override
    public void refund(String orderNo, Long paymentNo, String tid, Integer cancelAmount, Integer remainAmount) {
        try {
            // 전체 취소 vs 부분 취소 판단
            if (remainAmount == 0) {
                // 전체 취소 (로깅은 Client에서 처리)
                inicisClient.refund(orderNo, paymentNo, tid);
            } else {
                // 부분 취소 (로깅은 Client에서 처리)
                inicisClient.partialRefund(orderNo, paymentNo, tid, cancelAmount, remainAmount);
            }

            log.info("이니시스 취소 완료: tid={}, amount={}", tid, cancelAmount);

        } catch (Exception e) {
            log.error("이니시스 취소 실패", e);
            throw e;
        }
    }
}
