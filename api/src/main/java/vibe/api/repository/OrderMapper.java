package vibe.api.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vibe.api.dto.response.OrderCompleteResponse;
import vibe.api.dto.response.OrderDetailResponse;
import vibe.api.dto.response.OrderFormResponse;
import vibe.api.dto.response.OrderHistoryResponse;

import java.util.List;

/**
 * 주문 Mapper (조회 전용)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface OrderMapper {
    /**
     * 주문서 데이터 조회 (회원 정보 + 장바구니 리스트)
     */
    OrderFormResponse selectOrderFormData(@Param("memberNo") String memberNo);

    /**
     * 주문번호 채번
     * 형식: O + YYYYMMDD + 시퀀스 6자리
     */
    String selectNextOrderNo();

    /**
     * 주문 내역 조회
     */
    List<OrderHistoryResponse> selectOrderHistoryList(@Param("memberNo") String memberNo);

    /**
     * 주문 완료 조회
     */
    OrderCompleteResponse selectOrderCompleteData(@Param("orderNo") String orderNo);

    /**
     * 주문 상세 조회
     */
    OrderDetailResponse selectOrderDetail(@Param("orderNo") String orderNo);

    /**
     * 클레임번호 채번
     * 형식: C + YYYYMMDD + 시퀀스 6자리
     */
    String selectNextClaimNo();
}
