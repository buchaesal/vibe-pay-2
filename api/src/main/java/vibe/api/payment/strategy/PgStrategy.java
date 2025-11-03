package vibe.api.payment.strategy;

import java.util.Map;

/**
 * PG사 전략 인터페이스
 *
 * PG사(INICIS, TOSS 등)별로 전략을 구현합니다.
 * 카드 결제 시에만 사용됩니다.
 *
 * @author Claude
 * @version 1.0
 * @since 2025-11-01
 */
public interface PgStrategy {

    /**
     * 이 전략이 해당 PG사를 지원하는지 확인
     *
     * @param pgType PG사 타입 (INICIS, TOSS 등)
     * @return 지원 여부
     */
    boolean supports(String pgType);

    /**
     * PG사 승인 요청
     *
     * @param orderNo 주문번호
     * @param paymentNo 결제번호
     * @param authResult 인증 결과
     * @return PG사 승인 결과
     */
    Map<String, Object> approve(String orderNo, Long paymentNo, Map<String, Object> authResult);

    /**
     * PG사 망취소
     *
     * @param orderNo 주문번호
     * @param paymentNo 결제번호
     * @param authResult 인증 결과
     */
    void netCancel(String orderNo, Long paymentNo, Map<String, Object> authResult);

    /**
     * PG사 취소 (전체 또는 부분)
     *
     * @param orderNo 주문번호
     * @param paymentNo 결제번호
     * @param tid 거래ID
     * @param cancelAmount 취소금액
     * @param remainAmount 잔여금액
     * @param originalAmount 원결제금액
     */
    void refund(String orderNo, Long paymentNo, String tid, Integer cancelAmount, Integer remainAmount, Integer originalAmount);
}