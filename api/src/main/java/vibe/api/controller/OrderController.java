package vibe.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vibe.api.common.dto.Response;
import vibe.api.dto.request.OrderCancelRequest;
import vibe.api.dto.response.OrderCompleteResponse;
import vibe.api.dto.response.OrderDetailResponse;
import vibe.api.dto.response.OrderFormResponse;
import vibe.api.dto.response.OrderHistoryResponse;
import vibe.api.dto.response.OrderSequenceResponse;
import vibe.api.service.OrderService;

import java.util.List;

/**
 * 주문 컨트롤러
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문서 조회 API
     * 회원 정보 + 장바구니 목록 통합 조회
     */
    @GetMapping("/form")
    public Response<OrderFormResponse> getOrderForm(@RequestParam String memberNo) {
        log.info("주문서 조회 요청: memberNo={}", memberNo);

        OrderFormResponse response = orderService.getOrderFormData(memberNo);

        return new Response<>(response);
    }

    /**
     * 주문번호 채번 API
     * 형식: O + YYYYMMDD + 시퀀스 6자리
     */
    @PostMapping("/sequence")
    public Response<OrderSequenceResponse> getOrderSequence() {
        log.info("주문번호 채번 요청");

        OrderSequenceResponse response = orderService.getNextOrderNo();

        return new Response<>(response);
    }

    /**
     * 주문 내역 조회 API
     * GET /api/orders/history?memberNo=M001
     */
    @GetMapping("/history")
    public Response<List<OrderHistoryResponse>> getOrderHistory(@RequestParam String memberNo) {
        log.info("주문 내역 조회 요청: memberNo={}", memberNo);

        List<OrderHistoryResponse> response = orderService.getOrderHistory(memberNo);

        return new Response<>(response);
    }

    /**
     * 주문 완료 조회 API
     * GET /api/orders/complete?orderNo=O202510300001
     */
    @GetMapping("/complete")
    public Response<OrderCompleteResponse> getOrderComplete(@RequestParam String orderNo) {
        log.info("주문 완료 조회 요청: orderNo={}", orderNo);

        OrderCompleteResponse response = orderService.getOrderComplete(orderNo);

        return new Response<>(response);
    }

    /**
     * 주문 상세 조회 API
     * GET /api/orders/{orderNo}
     */
    @GetMapping("/{orderNo}")
    public Response<OrderDetailResponse> getOrderDetail(@PathVariable String orderNo) {
        log.info("주문 상세 조회 요청: orderNo={}", orderNo);

        OrderDetailResponse response = orderService.getOrderDetail(orderNo);

        return new Response<>(response);
    }

    /**
     * 주문 취소 API
     * POST /api/orders/cancel
     */
    @PostMapping("/cancel")
    public Response<Void> cancelOrder(@RequestBody OrderCancelRequest request) {
        log.info("주문 취소 요청: orderNo={}, orderSeq={}, cancelQty={}",
            request.getOrderNo(), request.getOrderSeq(), request.getCancelQty());

        orderService.cancelOrder(request);

        return new Response<>();
    }
}
