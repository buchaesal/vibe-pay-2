package vibepay.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vibepay.api.common.dto.Response;
import vibepay.api.dto.request.AddToCartRequest;
import vibepay.api.dto.request.DeleteCartRequest;
import vibepay.api.dto.response.CartResponse;
import vibepay.api.service.CartService;

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
    public Response<List<CartResponse>> getCartList(@RequestParam String memberNo) {
        log.info("장바구니 목록 조회 요청: memberNo={}", memberNo);

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
     * 장바구니 삭제 API
     */
    @DeleteMapping
    public Response<Void> deleteCart(@RequestBody @Valid DeleteCartRequest request) {
        log.info("장바구니 삭제 요청: cartIdCount={}", request.getCartIdList().size());

        cartService.deleteCart(request);

        return new Response<>();
    }
}
