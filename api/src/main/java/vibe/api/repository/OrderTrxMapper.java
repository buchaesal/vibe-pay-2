package vibe.api.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vibe.api.entity.OrderBase;
import vibe.api.entity.OrderDetail;
import vibe.api.entity.OrderProduct;

/**
 * 주문 Mapper (CUD)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface OrderTrxMapper {
    /**
     * 주문 기본 INSERT
     */
    void insertOrderBase(OrderBase orderBase);

    /**
     * 주문 상품 INSERT
     */
    void insertOrderProduct(OrderProduct orderProduct);

    /**
     * 주문 상세 INSERT
     */
    void insertOrderDetail(OrderDetail orderDetail);

    /**
     * 주문 상세 완료일시 업데이트
     */
    void updateOrderDetailComplete(@Param("orderNo") String orderNo);

    /**
     * 주문 상세 취소수량 누적 업데이트
     */
    void updateOrderDetailCancelQty(@Param("orderNo") String orderNo, @Param("orderSeq") Integer orderSeq, @Param("cancelQty") Integer cancelQty);
}
