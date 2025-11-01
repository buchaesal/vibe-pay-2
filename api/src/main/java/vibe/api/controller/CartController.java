package vibe.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vibe.api.common.dto.Response;
import vibe.api.dto.request.AddToCartRequest;
import vibe.api.dto.request.DeleteCartRequest;
import vibe.api.dto.request.UpdateCartQtyRequest;
import vibe.api.dto.response.CartResponse;
import vibe.api.service.CartService;

import java.util.List;

/**
 * 장바구니 컨트롤러
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 장바구니 목록 조회 API
     */
    @GetMapping
    public Response<List<CartResponse>> getCartList(@RequestParam(required = true) String memberNo) {
        log.info("장바구니 목록 조회 요청: memberNo={}", memberNo);

        // memberNo 유효성 검증
        if (memberNo == null || memberNo.trim().isEmpty()) {
            log.warn("memberNo가 비어있습니다.");
            throw new IllegalArgumentException("memberNo는 필수입니다.");
        }

        List<CartResponse> cartList = cartService.getCartList(memberNo);

        return new Response<>(cartList);
    }

    /**
     * 장바구니 담기 API
     */
    @PostMapping
    public Response<Void> addToCart(@RequestBody @Valid AddToCartRequest request) {
        log.info("장바구니 담기 요청: memberNo={}, productCount={}",
            request.getMemberNo(), request.getProductNoList().size());

        cartService.addToCart(request);

        return new Response<>();
    }

    /**
     * 장바구니 수량 변경 API
     */
    @PutMapping("/qty")
    public Response<Void> updateCartQty(@RequestBody @Valid UpdateCartQtyRequest request) {
        log.info("장바구니 수량 변경 요청: cartId={}, qty={}", request.getCartId(), request.getQty());

        cartService.updateCartQty(request);

        return new Response<>();
    }

    /**
     * 장바구니 삭제 API
     */
    @DeleteMapping
    public Response<Void> deleteCart(@RequestBody @Valid DeleteCartRequest request) {
        log.info("장바구니 삭제 요청: cartIdCount={}", request.getCartIdList().size());

        cartService.deleteCart(request);

        return new Response<>();
    }
}
