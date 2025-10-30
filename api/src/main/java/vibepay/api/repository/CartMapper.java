package vibepay.api.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vibepay.api.dto.response.CartResponse;

import java.util.List;

/**
 * 장바구니 Mapper (조회 전용)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface CartMapper {
    /**
     * 장바구니 목록 조회
     */
    List<CartResponse> selectCartList(@Param("memberNo") String memberNo);

    /**
     * 장바구니 존재 여부 확인
     */
    int countByMemberNoAndProductNo(@Param("memberNo") String memberNo, @Param("productNo") String productNo);

    /**
     * 장바구니 ID 리스트로 조회
     */
    List<CartResponse> selectCartByIds(@Param("cartIdList") List<Long> cartIdList);
}
