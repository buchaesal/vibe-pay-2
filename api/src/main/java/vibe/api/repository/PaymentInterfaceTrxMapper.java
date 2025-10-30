package vibe.api.repository;

import org.apache.ibatis.annotations.Mapper;
import vibe.api.entity.PaymentInterface;

/**
 * 결제 인터페이스 Mapper (CUD)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface PaymentInterfaceTrxMapper {
    /**
     * 결제 인터페이스 로그 INSERT
     */
    void insertPaymentInterface(PaymentInterface paymentInterface);
}
