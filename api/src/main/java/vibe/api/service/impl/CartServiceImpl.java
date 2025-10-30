package vibe.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;
import vibe.api.dto.request.AddToCartRequest;
import vibe.api.dto.request.DeleteCartRequest;
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
        log.debug("장바구니 담기: memberNo={}, productCount={}",
            request.getMemberNo(), request.getProductNoList().size());

        for (String productNo : request.getProductNoList()) {
            // 상품 존재 여부 확인
            productMapper.selectProductByProductNo(productNo)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));

            // 장바구니 추가 (ON CONFLICT 시 수량 증가)
            Cart cart = new Cart();
            cart.setMemberNo(request.getMemberNo());
            cart.setProductNo(productNo);
            cart.setQty(1);

            cartTrxMapper.insertCart(cart);
        }

        log.info("장바구니 담기 성공: memberNo={}, productCount={}",
            request.getMemberNo(), request.getProductNoList().size());
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
}
