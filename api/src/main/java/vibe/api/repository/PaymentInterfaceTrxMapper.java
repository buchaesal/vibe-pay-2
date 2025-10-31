package vibe.api.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
     * @return interfaceSeq (PK)
     */
    Long insertPaymentInterface(PaymentInterface paymentInterface);

    /**
     * 결제 인터페이스 로그 UPDATE (응답값 업데이트)
     */
    void updatePaymentInterfaceResponse(@Param("interfaceSeq") Long interfaceSeq,
                                        @Param("responseJson") String responseJson,
                                        @Param("resultCode") String resultCode);
}
