package vibe.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vibe.api.entity.PaymentInterface;
import vibe.api.repository.PaymentInterfaceTrxMapper;

import java.time.LocalDateTime;

/**
 * 결제 인터페이스 로그 서비스
 *
 * PG사 통신 로그를 별도 트랜잭션으로 저장/업데이트
 * InicisClient, TossClient 등에서 공통으로 사용
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentInterfaceService {

    private final PaymentInterfaceTrxMapper paymentInterfaceTrxMapper;
    private final ObjectMapper objectMapper;

    /**
     * 인터페이스 요청 로그 저장 (트랜잭션 분리)
     *
     * @param pgType PG 타입 (INICIS, TOSS)
     * @param transactionType 거래 타입 (APPROVAL, CANCEL, NET_CANCEL)
     * @param orderNo 주문번호
     * @param paymentNo 결제번호
     * @param request 요청 데이터
     * @return interfaceSeq (PK)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long saveRequest(String pgType, String transactionType, String orderNo, Long paymentNo, Object request) {
        try {
            PaymentInterface paymentInterface = new PaymentInterface();
            paymentInterface.setPgType(pgType);
            paymentInterface.setTransactionType(transactionType);
            paymentInterface.setOrderNo(orderNo);
            paymentInterface.setPaymentNo(paymentNo);
            paymentInterface.setRequestJson(objectMapper.writeValueAsString(request));
            paymentInterface.setResponseJson(null);
            paymentInterface.setResultCode(null);
            paymentInterface.setTransactionDatetime(LocalDateTime.now());

            paymentInterfaceTrxMapper.insertPaymentInterface(paymentInterface);

            return paymentInterface.getInterfaceSeq();

        } catch (Exception e) {
            log.error("PAYMENT_INTERFACE 요청 저장 실패 (무시)", e);
            return null;
        }
    }

    /**
     * 인터페이스 응답 로그 업데이트 (트랜잭션 분리)
     *
     * @param interfaceSeq 인터페이스 SEQ
     * @param response 응답 데이터
     * @param resultCode 결과 코드 (이니시스: resultCode, 토스: status 또는 code)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateResponse(Long interfaceSeq, Object response, String resultCode) {
        if (interfaceSeq == null) {
            return;
        }

        try {
            String responseJson = objectMapper.writeValueAsString(response);
            paymentInterfaceTrxMapper.updatePaymentInterfaceResponse(interfaceSeq, responseJson, resultCode);

        } catch (Exception e) {
            log.error("PAYMENT_INTERFACE 응답 업데이트 실패 (무시)", e);
        }
    }
}
