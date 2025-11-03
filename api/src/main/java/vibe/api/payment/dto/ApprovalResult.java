package vibe.api.payment.dto;

import lombok.Getter;
import vibe.api.entity.Payment;

/**
 * 결제 승인 결과
 *
 * 각 결제 수단 전략이 승인 결과와 망취소 필요 여부를 반환하기 위한 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-11-03
 */
@Getter
public class ApprovalResult {

    private final Payment payment;
    private final boolean needsNetCancel;
    private final Object netCancelContext;
    private final boolean success;

    private ApprovalResult(Payment payment, boolean needsNetCancel, Object netCancelContext, boolean success) {
        this.payment = payment;
        this.needsNetCancel = needsNetCancel;
        this.netCancelContext = netCancelContext;
        this.success = success;
    }

    /**
     * 승인 성공
     *
     * @param payment Payment 객체
     * @param needsNetCancel 망취소 필요 여부 (카드: true, 적립금: false)
     * @param netCancelContext 망취소용 컨텍스트 (카드: PG 승인 정보, 적립금: null)
     */
    public static ApprovalResult success(Payment payment, boolean needsNetCancel, Object netCancelContext) {
        return new ApprovalResult(payment, needsNetCancel, netCancelContext, true);
    }

    /**
     * 승인 실패 (PG 승인은 성공했으나 후속 처리 실패)
     *
     * @param needsNetCancel 망취소 필요 여부
     * @param netCancelContext 망취소용 컨텍스트
     */
    public static ApprovalResult failed(boolean needsNetCancel, Object netCancelContext) {
        return new ApprovalResult(null, needsNetCancel, netCancelContext, false);
    }
}
