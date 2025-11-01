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
     * 회원 정보 + 선택한 장바구니 상품 목록 통합 조회
     */
    @Override
    public OrderFormResponse getOrderFormData(String memberNo, List<Long> cartIdList) {
        log.debug("주문서 데이터 조회: memberNo={}, cartIdList={}", memberNo, cartIdList);

        OrderFormResponse response = orderMapper.selectOrderFormData(memberNo, cartIdList);

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

        log.info("주문서 데이터 조회 성공: memberNo={}, cartIdList={}, totalAmount={}", memberNo, cartIdList, totalAmount);

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
     * 7. 장바구니 삭제
     */
    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        String orderNo = request.getOrderInfo().getOrderNo();
        log.info("주문 생성 시작: orderNo={}, memberNo={}", orderNo, request.getOrderInfo().getMemberNo());

        List<CartResponse> cartItems = validateAndGetCartItems(request.getOrderInfo().getCartIdList());
        LocalDateTime orderDatetime = LocalDateTime.now();

        createOrderBase(request, orderNo, orderDatetime);
        createOrderProducts(cartItems, orderNo);
        createOrderDetails(cartItems, orderNo, orderDatetime);

        processPayment(request, orderNo);

        completeOrder(orderNo, request.getOrderInfo().getCartIdList());

        log.info("주문 생성 완료: orderNo={}", orderNo);
        return new CreateOrderResponse(orderNo, "SUCCESS");
    }

    /**
     * 장바구니 검증 및 조회
     */
    private List<CartResponse> validateAndGetCartItems(List<Long> cartIdList) {
        List<CartResponse> cartItems = cartMapper.selectCartByIds(cartIdList);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new ApiException(ErrorCode.CART_NOT_FOUND);
        }
        log.debug("장바구니 조회 완료: count={}", cartItems.size());
        return cartItems;
    }

    /**
     * ORDER_BASE 생성
     */
    private void createOrderBase(CreateOrderRequest request, String orderNo, LocalDateTime orderDatetime) {
        OrderBase orderBase = new OrderBase();
        orderBase.setOrderNo(orderNo);
        orderBase.setMemberNo(request.getOrderInfo().getMemberNo());
        orderBase.setOrderDatetime(orderDatetime);
        orderBase.setOrdererName(request.getOrderInfo().getOrdererName());
        orderBase.setOrdererPhone(request.getOrderInfo().getOrdererPhone());
        orderBase.setOrdererEmail(request.getOrderInfo().getOrdererEmail());

        orderTrxMapper.insertOrderBase(orderBase);
        log.debug("ORDER_BASE 생성 완료: orderNo={}", orderNo);
    }

    /**
     * ORDER_PRODUCT 생성 (상품별 스냅샷)
     */
    private void createOrderProducts(List<CartResponse> cartItems, String orderNo) {
        for (CartResponse cart : cartItems) {
            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setOrderNo(orderNo);
            orderProduct.setProductNo(cart.getProductNo());
            orderProduct.setProductName(cart.getProductName());
            orderProduct.setPrice(cart.getPrice());

            orderTrxMapper.insertOrderProduct(orderProduct);
        }
        log.debug("ORDER_PRODUCT 생성 완료: count={}", cartItems.size());
    }

    /**
     * ORDER_DETAIL 생성 (상품별 주문 상세)
     */
    private void createOrderDetails(List<CartResponse> cartItems, String orderNo, LocalDateTime orderDatetime) {
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
            orderDetail.setCompleteDatetime(null);
            orderDetail.setOrderQty(cart.getQty());
            orderDetail.setCancelQty(0);

            orderTrxMapper.insertOrderDetail(orderDetail);
            orderSeq++;
        }
        log.debug("ORDER_DETAIL 생성 완료: count={}", cartItems.size());
    }

    /**
     * 결제 처리
     */
    private void processPayment(CreateOrderRequest request, String orderNo) {
        try {
            paymentService.processPayments(request);
            log.info("결제 처리 완료: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("결제 처리 실패: orderNo={}", orderNo, e);
            throw e;  // 트랜잭션 롤백
        }
    }

    /**
     * 주문 완료 처리
     */
    private void completeOrder(String orderNo, List<Long> cartIdList) {
        orderTrxMapper.updateOrderDetailComplete(orderNo);
        log.debug("ORDER_DETAIL 완료일시 업데이트: orderNo={}", orderNo);

        cartTrxMapper.deleteCartByIds(cartIdList);
        log.debug("장바구니 삭제 완료: count={}", cartIdList.size());
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
        String orderNo = request.getOrderNo();
        Boolean isFullCancel = request.getIsFullCancel() != null && request.getIsFullCancel();

        if (isFullCancel) {
            log.info("전체 주문 취소 시작: orderNo={}", orderNo);
            processFullCancel(orderNo);
        } else {
            log.info("부분 주문 취소 시작: orderNo={}, orderSeq={}, cancelQty={}",
                orderNo, request.getOrderSeq(), request.getCancelQty());
            processPartialCancel(request);
        }
    }

    /**
     * 전체 주문 취소 처리
     */
    private void processFullCancel(String orderNo) {
        // 1. 클레임번호 채번
        String claimNo = orderMapper.selectNextClaimNo();

        // 2. 주문 상세 조회
        OrderDetailResponse orderDetail = orderMapper.selectOrderDetail(orderNo);
        if (orderDetail == null || orderDetail.getItems().isEmpty()) {
            throw new ApiException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 3. 취소 가능한 상품만 필터링
        List<OrderDetailResponse.OrderDetailItem> cancellableItems = orderDetail.getItems().stream()
            .filter(item -> item.getQty() > item.getCancelQty())
            .toList();

        if (cancellableItems.isEmpty()) {
            throw new ApiException(ErrorCode.CANCEL_FAIL);
        }

        // 4. 총 취소 금액 계산
        Integer totalCancelAmount = 0;
        int processSeq = orderDetail.getItems().size() + 1;

        // 5. 각 상품별 취소 처리
        for (OrderDetailResponse.OrderDetailItem item : cancellableItems) {
            Integer availableQty = item.getQty() - item.getCancelQty();
            Integer cancelAmount = item.getPrice() * availableQty;
            totalCancelAmount += cancelAmount;

            // 취소 주문 상세 INSERT
            OrderDetail cancelDetail = new OrderDetail();
            cancelDetail.setOrderNo(orderNo);
            cancelDetail.setOrderSeq(item.getOrderSeq());
            cancelDetail.setProcessSeq(processSeq++);
            cancelDetail.setParentProcessSeq(item.getOrderSeq());
            cancelDetail.setClaimNo(claimNo);
            cancelDetail.setProductNo(item.getProductNo());
            cancelDetail.setOrderType("CANCEL");
            cancelDetail.setOrderDatetime(LocalDateTime.now());
            cancelDetail.setCompleteDatetime(LocalDateTime.now());
            cancelDetail.setOrderQty(availableQty);
            cancelDetail.setCancelQty(0);

            orderTrxMapper.insertOrderDetail(cancelDetail);

            // 원주문의 취소수량 누적 업데이트
            orderTrxMapper.updateOrderDetailCancelQty(orderNo, item.getOrderSeq(), availableQty);

            log.debug("상품 취소 처리: orderSeq={}, qty={}, amount={}",
                item.getOrderSeq(), availableQty, cancelAmount);
        }

        // 6. 결제 취소 처리
        paymentService.processRefund(orderNo, totalCancelAmount);

        log.info("전체 주문 취소 완료: orderNo={}, claimNo={}, totalAmount={}",
            orderNo, claimNo, totalCancelAmount);
    }

    /**
     * 부분 주문 취소 처리
     */
    private void processPartialCancel(OrderCancelRequest request) {
        String orderNo = request.getOrderNo();
        Integer orderSeq = request.getOrderSeq();
        Integer cancelQty = request.getCancelQty();

        // 1. 클레임번호 채번
        String claimNo = orderMapper.selectNextClaimNo();

        // 2. 주문 상세 조회 (원주문 정보)
        OrderDetailResponse orderDetail = orderMapper.selectOrderDetail(orderNo);
        if (orderDetail == null || orderDetail.getItems().isEmpty()) {
            throw new ApiException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 3. 취소할 상품 찾기
        OrderDetailResponse.OrderDetailItem targetItem = orderDetail.getItems().stream()
            .filter(item -> item.getOrderSeq().equals(orderSeq))
            .findFirst()
            .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));

        // 4. 취소 가능 수량 검증
        Integer availableQty = targetItem.getQty() - targetItem.getCancelQty();
        if (availableQty < cancelQty) {
            throw new ApiException(ErrorCode.CANCEL_FAIL);
        }

        // 5. 취소 금액 계산
        Integer cancelAmount = targetItem.getPrice() * cancelQty;

        // 6. 취소 주문 상세 INSERT
        OrderDetail cancelDetail = new OrderDetail();
        cancelDetail.setOrderNo(orderNo);
        cancelDetail.setOrderSeq(orderSeq);
        cancelDetail.setProcessSeq(orderDetail.getItems().size() + 1);  // 새로운 processSeq
        cancelDetail.setParentProcessSeq(orderSeq);
        cancelDetail.setClaimNo(claimNo);
        cancelDetail.setProductNo(targetItem.getProductNo());
        cancelDetail.setOrderType("CANCEL");
        cancelDetail.setOrderDatetime(LocalDateTime.now());
        cancelDetail.setCompleteDatetime(LocalDateTime.now());
        cancelDetail.setOrderQty(0);
        cancelDetail.setCancelQty(cancelQty);

        orderTrxMapper.insertOrderDetail(cancelDetail);

        // 7. 원주문의 취소수량 누적 업데이트
        orderTrxMapper.updateOrderDetailCancelQty(orderNo, orderSeq, cancelQty);

        // 8. 결제 취소 처리
        paymentService.processRefund(orderNo, cancelAmount);

        log.info("부분 주문 취소 완료: orderNo={}, claimNo={}", orderNo, claimNo);
    }
}
