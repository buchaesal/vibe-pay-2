package vibepay.api.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vibepay.api.entity.Payment;

import java.util.List;

/**
 * 결제 Mapper (조회 전용)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface PaymentMapper {
    /**
     * 주문번호로 결제 목록 조회 (환불가능금액이 있는 것만)
     */
    List<Payment> selectPaymentListByOrderNo(@Param("orderNo") String orderNo);
}
