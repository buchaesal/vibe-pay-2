package vibepay.api.service;

import vibepay.api.dto.request.AddToCartRequest;
import vibepay.api.dto.request.DeleteCartRequest;
import vibepay.api.dto.response.CartResponse;

import java.util.List;

/**
 * 장바구니 서비스 인터페이스
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
public interface CartService {
    /**
     * 장바구니 목록 조회
     */
    List<CartResponse> getCartList(String memberNo);

    /**
     * 장바구니 담기
     */
    void addToCart(AddToCartRequest request);

    /**
     * 장바구니 삭제
     */
    void deleteCart(DeleteCartRequest request);
}
