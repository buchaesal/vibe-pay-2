package vibe.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;
import vibe.api.dto.request.AddToCartRequest;
import vibe.api.dto.request.DeleteCartRequest;
import vibe.api.dto.request.UpdateCartQtyRequest;
import vibe.api.dto.response.CartResponse;
import vibe.api.entity.Cart;
import vibe.api.repository.CartMapper;
import vibe.api.repository.CartTrxMapper;
import vibe.api.repository.ProductMapper;
import vibe.api.service.CartService;

import java.util.List;

/**
 * 장바구니 서비스 구현
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final CartTrxMapper cartTrxMapper;
    private final ProductMapper productMapper;

    /**
     * 장바구니 목록 조회
     */
    @Override
    public List<CartResponse> getCartList(String memberNo) {
        log.debug("장바구니 목록 조회: memberNo={}", memberNo);

        return cartMapper.selectCartList(memberNo);
    }

    /**
     * 장바구니 담기
     * 동일 상품이 존재하면 수량 증가 (ON CONFLICT)
     */
    @Override
    @Transactional
    public void addToCart(AddToCartRequest request) {
        log.debug("장바구니 담기: memberNo={}, itemCount={}",
            request.getMemberNo(), request.getItems().size());

        for (AddToCartRequest.CartItemRequest item : request.getItems()) {
            // 상품 존재 여부 확인
            productMapper.selectProductByProductNo(item.getProductNo())
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));

            // 장바구니 추가 (ON CONFLICT 시 지정된 수량만큼 증가)
            Cart cart = new Cart();
            cart.setMemberNo(request.getMemberNo());
            cart.setProductNo(item.getProductNo());
            cart.setQty(item.getQty());

            cartTrxMapper.insertCart(cart);

            log.debug("상품 장바구니 추가: productNo={}, qty={}", item.getProductNo(), item.getQty());
        }

        log.info("장바구니 담기 성공: memberNo={}, itemCount={}",
            request.getMemberNo(), request.getItems().size());
    }

    /**
     * 장바구니 수량 변경
     */
    @Override
    @Transactional
    public void updateCartQty(UpdateCartQtyRequest request) {
        log.debug("장바구니 수량 변경: cartId={}, qty={}", request.getCartId(), request.getQty());

        int updatedCount = cartTrxMapper.updateCartQtyByCartId(request.getCartId(), request.getQty());

        if (updatedCount == 0) {
            throw new ApiException(ErrorCode.INVALID_CART_ID);
        }

        log.info("장바구니 수량 변경 성공: cartId={}, qty={}", request.getCartId(), request.getQty());
    }

    /**
     * 장바구니 삭제
     */
    @Override
    @Transactional
    public void deleteCart(DeleteCartRequest request) {
        log.debug("장바구니 삭제: cartIdCount={}", request.getCartIdList().size());

        int deletedCount = cartTrxMapper.deleteCartByIds(request.getCartIdList());

        if (deletedCount == 0) {
            throw new ApiException(ErrorCode.INVALID_CART_ID);
        }

        log.info("장바구니 삭제 성공: deletedCount={}", deletedCount);
    }

    /**
     * 주문 완료 후 장바구니 삭제 (별도 트랜잭션)
     * REQUIRES_NEW: 새로운 트랜잭션을 시작하여 부모 트랜잭션과 독립적으로 동작
     * 실패해도 예외를 던지지 않음 (주문 완료에 영향을 주지 않기 위함)
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteCartAfterOrder(List<Long> cartIdList) {
        if (cartIdList == null || cartIdList.isEmpty()) {
            log.warn("주문 완료 후 장바구니 삭제: cartIdList가 비어있음");
            return;
        }

        try {
            log.debug("주문 완료 후 장바구니 삭제 시작: cartIdCount={}", cartIdList.size());

            int deletedCount = cartTrxMapper.deleteCartByIds(cartIdList);

            log.info("주문 완료 후 장바구니 삭제 성공: deletedCount={}", deletedCount);
        } catch (Exception e) {
            // 예외를 던지지 않고 로그만 남김 (주문 완료는 정상 처리되어야 함)
            log.error("주문 완료 후 장바구니 삭제 실패: cartIdList={}, error={}",
                cartIdList, e.getMessage(), e);
        }
    }
}
