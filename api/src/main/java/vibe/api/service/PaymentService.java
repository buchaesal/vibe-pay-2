package vibe.api.service;

import vibe.api.dto.request.CreateOrderRequest;
import vibe.api.dto.response.PaymentParamsResponse;
import vibe.api.entity.Payment;

import java.util.List;

/**
 * 결제 서비스 인터페이스
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
public interface PaymentService {
    /**
     * 이니시스 인증 파라미터 생성
     */
    PaymentParamsResponse generateInicisAuthParams(String orderNo, Integer price);

    /**
     * 결제 처리 (전략 패턴)
     */
    void processPayments(CreateOrderRequest orderRequest);

    /**
     * 결제 취소 처리
     * @param orderNo 주문번호
     * @param cancelAmount 취소 금액
     * @return 취소된 결제 목록
     */
    List<Payment> processRefund(String orderNo, Integer cancelAmount);
}
