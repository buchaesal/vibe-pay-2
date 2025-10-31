package vibe.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vibe.api.common.dto.Response;
import vibe.api.dto.request.CreateOrderRequest;
import vibe.api.dto.response.CreateOrderResponse;
import vibe.api.dto.response.PaymentParamsResponse;
import vibe.api.service.OrderService;
import vibe.api.service.PaymentService;

/**
 * 결제 컨트롤러
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    /**
     * 이니시스 인증 파라미터 생성
     * GET /api/payment/params?orderNo=O20251030000001&price=10000
     */
    @GetMapping("/payment/params")
    public Response<PaymentParamsResponse> getPaymentParams(
        @RequestParam String orderNo,
        @RequestParam Integer price
    ) {
        log.info("이니시스 인증 파라미터 요청: orderNo={}, price={}", orderNo, price);

        PaymentParamsResponse response = paymentService.generateInicisAuthParams(orderNo, price);

        return new Response<>(response);
    }

    /**
     * 주문 생성 (결제 포함)
     * POST /api/orders
     *
     * 요청 Body:
     * {
     *   "orderNo": "O20251030000001",
     *   "memberNo": "M0000001",
     *   "ordererName": "홍길동",
     *   "ordererPhone": "01012345678",
     *   "ordererEmail": "hong@example.com",
     *   "payments": [
     *     {
     *       "pgType": "INICIS",
     *       "method": "CARD",
     *       "amount": 10000,
     *       "authResult": { ... }
     *     }
     *   ],
     *   "cartIdList": [1, 2, 3]
     * }
     */
    @PostMapping("/orders")
    public Response<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderService.createOrder(request);
        return new Response<>(response);
    }
}
