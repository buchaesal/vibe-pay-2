package vibe.api.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vibe.api.entity.Cart;

import java.util.List;

/**
 * 장바구니 TrxMapper (등록/수정/삭제)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface CartTrxMapper {
    /**
     * 장바구니 추가 (ON CONFLICT 수량 증가)
     */
    int insertCart(Cart cart);

    /**
     * 장바구니 수량 증가
     */
    int updateCartQty(@Param("memberNo") String memberNo, @Param("productNo") String productNo);

    /**
     * 장바구니 수량 변경 (특정 수량으로 설정)
     */
    int updateCartQtyByCartId(@Param("cartId") Long cartId, @Param("qty") Integer qty);

    /**
     * 장바구니 삭제 (여러 개)
     */
    int deleteCartByIds(@Param("cartIdList") List<Long> cartIdList);

    /**
     * 장바구니 전체 삭제 (회원별)
     */
    int deleteCartByMemberNo(@Param("memberNo") String memberNo);
}
