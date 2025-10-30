package vibe.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;
import vibe.api.dto.request.CreateOrderRequest;
import vibe.api.dto.request.OrderCancelRequest;
import vibe.api.dto.response.CartResponse;
import vibe.api.dto.response.CreateOrderResponse;
import vibe.api.dto.response.OrderCompleteResponse;
import vibe.api.dto.response.OrderDetailResponse;
import vibe.api.dto.response.OrderFormResponse;
import vibe.api.dto.response.OrderHistoryResponse;
import vibe.api.dto.response.OrderSequenceResponse;
import vibe.api.entity.OrderBase;
import vibe.api.entity.OrderDetail;
import vibe.api.entity.OrderProduct;
import vibe.api.repository.CartMapper;
import vibe.api.repository.CartTrxMapper;
import vibe.api.repository.OrderMapper;
import vibe.api.repository.OrderTrxMapper;
import vibe.api.service.OrderService;
import vibe.api.service.PaymentService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주문 서비스 구현
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderTrxMapper orderTrxMapper;
    private final CartMapper cartMapper;
    private final CartTrxMapper cartTrxMapper;
    private final PaymentService paymentService;

    /**
     * 주문서 데이터 조회
     * 회원 정보 + 장바구니 목록 통합 조회
     */
    @Override
    public OrderFormResponse getOrderFormData(String memberNo) {
        log.debug("주문서 데이터 조회: memberNo={}", memberNo);

        OrderFormResponse response = orderMapper.selectOrderFormData(memberNo);

        if (response == null || response.getMemberInfo() == null) {
            throw new ApiException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // totalAmount 계산
        int totalAmount = 0;
        if (response.getCartList() != null && !response.getCartList().isEmpty()) {
            totalAmount = response.getCartList().stream()
                .mapToInt(item -> item.getPrice() * item.getQty())
                .sum();
        }
        response.setTotalAmount(totalAmount);

        // 사용 가능한 결제수단 설정
        response.setAvailablePayments(Arrays.asList("CARD", "POINT"));

        log.info("주문서 데이터 조회 성공: memberNo={}, totalAmount={}", memberNo, totalAmount);

        return response;
    }

    /**
     * 주문번호 채번
     * 형식: O + YYYYMMDD + 시퀀스 6자리
     */
    @Override
    public OrderSequenceResponse getNextOrderNo() {
        log.debug("주문번호 채번 요청");

        String orderNo = orderMapper.selectNextOrderNo();

        log.info("주문번호 채번 성공: orderNo={}", orderNo);

        return new OrderSequenceResponse(orderNo);
    }

    /**
     * 주문 생성 (결제 포함)
     *
     * 처리 순서:
     * 1. 장바구니 조회 (cartIdList 기준)
     * 2. ORDER_BASE INSERT
     * 3. ORDER_PRODUCT INSERT (상품별 스냅샷)
     * 4. ORDER_DETAIL INSERT (상품별 주문 상세)
     * 5. 결제 처리 (PaymentService.processPayments)
     * 6. ORDER_DETAIL 완료일시 업데이트
     * 7. 장바구니 삭제 (별도 트랜잭션)
     */
    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        log.info("주문 생성 시작: orderNo={}, memberNo={}", request.getOrderNo(), request.getMemberNo());

        String orderNo = request.getOrderNo();
        LocalDateTime orderDatetime = LocalDateTime.now();

        // 1. 장바구니 조회
        List<CartResponse> cartItems = cartMapper.selectCartByIds(request.getCartIdList());
        if (cartItems == null || cartItems.isEmpty()) {
            throw new ApiException(ErrorCode.CART_NOT_FOUND);
        }

        log.debug("장바구니 조회 완료: count={}", cartItems.size());

        // 2. ORDER_BASE INSERT
        OrderBase orderBase = new OrderBase();
        orderBase.setOrderNo(orderNo);
        orderBase.setMemberNo(request.getMemberNo());
        orderBase.setOrderDatetime(orderDatetime);
        orderBase.setOrdererName(request.getOrdererName());
        orderBase.setOrdererPhone(request.getOrdererPhone());
        orderBase.setOrdererEmail(request.getOrdererEmail());

        orderTrxMapper.insertOrderBase(orderBase);
        log.debug("ORDER_BASE 생성 완료: orderNo={}", orderNo);

        // 3. ORDER_PRODUCT INSERT (상품별 스냅샷)
        Map<String, CartResponse> productMap = new HashMap<>();
        for (CartResponse cart : cartItems) {
            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setOrderNo(orderNo);
            orderProduct.setProductNo(cart.getProductNo());
            orderProduct.setProductName(cart.getProductName());
            orderProduct.setPrice(cart.getPrice());

            orderTrxMapper.insertOrderProduct(orderProduct);
            productMap.put(cart.getProductNo(), cart);
        }
        log.debug("ORDER_PRODUCT 생성 완료: count={}", cartItems.size());

        // 4. ORDER_DETAIL INSERT (상품별 주문 상세)
        int orderSeq = 1;
        for (CartResponse cart : cartItems) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderNo(orderNo);
            orderDetail.setOrderSeq(orderSeq);
            orderDetail.setProcessSeq(orderSeq);
            orderDetail.setParentProcessSeq(null);
            orderDetail.setClaimNo(null);
            orderDetail.setProductNo(cart.getProductNo());
            orderDetail.setOrderType("ORDER");
            orderDetail.setOrderDatetime(orderDatetime);
            orderDetail.setCompleteDatetime(null);  // 결제 완료 후 업데이트
            orderDetail.setOrderQty(cart.getQty());
            orderDetail.setCancelQty(0);

            orderTrxMapper.insertOrderDetail(orderDetail);
            orderSeq++;
        }
        log.debug("ORDER_DETAIL 생성 완료: count={}", cartItems.size());

        // 5. 결제 처리 (전략 패턴 + 망취소)
        try {
            paymentService.processPayments(orderNo, request.getPayments());
            log.info("결제 처리 완료: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("결제 처리 실패: orderNo={}", orderNo, e);
            throw e;  // 트랜잭션 롤백 (ORDER_BASE, ORDER_PRODUCT, ORDER_DETAIL 모두 롤백)
        }

        // 6. ORDER_DETAIL 완료일시 업데이트
        orderTrxMapper.updateOrderDetailComplete(orderNo);
        log.debug("ORDER_DETAIL 완료일시 업데이트: orderNo={}", orderNo);

        // 7. 장바구니 삭제 (별도 트랜잭션으로 분리하지 않고 같은 트랜잭션에서 처리)
        cartTrxMapper.deleteCartByIds(request.getCartIdList());
        log.debug("장바구니 삭제 완료: count={}", request.getCartIdList().size());

        log.info("주문 생성 완료: orderNo={}", orderNo);

        return new CreateOrderResponse(orderNo, "SUCCESS");
    }

    /**
     * 주문 내역 조회
     */
    @Override
    public List<OrderHistoryResponse> getOrderHistory(String memberNo) {
        log.debug("주문 내역 조회: memberNo={}", memberNo);

        List<OrderHistoryResponse> response = orderMapper.selectOrderHistoryList(memberNo);

        log.info("주문 내역 조회 완료: memberNo={}, count={}", memberNo, response.size());

        return response;
    }

    /**
     * 주문 완료 조회
     */
    @Override
    public OrderCompleteResponse getOrderComplete(String orderNo) {
        log.debug("주문 완료 조회: orderNo={}", orderNo);

        OrderCompleteResponse response = orderMapper.selectOrderCompleteData(orderNo);

        if (response == null) {
            throw new ApiException(ErrorCode.ORDER_NOT_FOUND);
        }

        log.info("주문 완료 조회 완료: orderNo={}", orderNo);

        return response;
    }

    /**
     * 주문 상세 조회
     */
    @Override
    public OrderDetailResponse getOrderDetail(String orderNo) {
        log.debug("주문 상세 조회: orderNo={}", orderNo);

        OrderDetailResponse response = orderMapper.selectOrderDetail(orderNo);

        if (response == null) {
            throw new ApiException(ErrorCode.ORDER_NOT_FOUND);
        }

        log.info("주문 상세 조회 완료: orderNo={}", orderNo);

        return response;
    }

    /**
     * 주문 취소
     */
    @Override
    @Transactional
    public void cancelOrder(OrderCancelRequest request) {
        log.info("주문 취소 시작: orderNo={}, orderSeq={}, cancelQty={}",
            request.getOrderNo(), request.getOrderSeq(), request.getCancelQty());

        // 1. 클레임번호 채번
        String claimNo = orderMapper.selectNextClaimNo();

        // 2. 주문 상세 조회 (원주문 정보)
        OrderDetailResponse orderDetail = orderMapper.selectOrderDetail(request.getOrderNo());
        if (orderDetail == null || orderDetail.getItems().isEmpty()) {
            throw new ApiException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 3. 취소할 상품 찾기
        OrderDetailResponse.OrderDetailItem targetItem = orderDetail.getItems().stream()
            .filter(item -> item.getOrderSeq().equals(request.getOrderSeq()))
            .findFirst()
            .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));

        // 4. 취소 가능 수량 검증
        Integer availableQty = targetItem.getQty() - targetItem.getCancelQty();
        if (availableQty < request.getCancelQty()) {
            throw new ApiException(ErrorCode.CANCEL_FAIL);
        }

        // 5. 취소 금액 계산
        Integer cancelAmount = targetItem.getPrice() * request.getCancelQty();

        // 6. 취소 주문 상세 INSERT
        OrderDetail cancelDetail = new OrderDetail();
        cancelDetail.setOrderNo(request.getOrderNo());
        cancelDetail.setOrderSeq(request.getOrderSeq());
        cancelDetail.setProcessSeq(orderDetail.getItems().size() + 1);  // 새로운 processSeq
        cancelDetail.setParentProcessSeq(request.getOrderSeq());
        cancelDetail.setClaimNo(claimNo);
        cancelDetail.setProductNo(targetItem.getProductNo());
        cancelDetail.setOrderType("CANCEL");
        cancelDetail.setOrderDatetime(LocalDateTime.now());
        cancelDetail.setCompleteDatetime(LocalDateTime.now());
        cancelDetail.setOrderQty(0);
        cancelDetail.setCancelQty(request.getCancelQty());

        orderTrxMapper.insertOrderDetail(cancelDetail);

        // 7. 원주문의 취소수량 누적 업데이트
        orderTrxMapper.updateOrderDetailCancelQty(request.getOrderNo(), request.getOrderSeq(), request.getCancelQty());

        // 8. 결제 취소 처리
        paymentService.processRefund(request.getOrderNo(), cancelAmount);

        log.info("주문 취소 완료: orderNo={}, claimNo={}", request.getOrderNo(), claimNo);
    }
}
