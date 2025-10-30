package vibepay.api.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vibepay.api.entity.Payment;

/**
 * 결제 Mapper (CUD)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface PaymentTrxMapper {
    /**
     * 결제 INSERT
     */
    void insertPayment(Payment payment);

    /**
     * 환불가능금액 업데이트
     */
    void updateRemainRefundableAmount(@Param("paymentNo") Long paymentNo, @Param("remainAmount") Integer remainAmount);
}
