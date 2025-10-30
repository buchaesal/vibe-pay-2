package vibepay.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 결제 인터페이스 Entity (PG 통신 로그)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class PaymentInterface {
    private Long interfaceSeq;
    private String pgType;  // INICIS, TOSS
    private String transactionType;  // APPROVAL, CANCEL, NET_CANCEL
    private LocalDateTime transactionDatetime;
    private String orderNo;
    private Long paymentNo;
    private String requestJson;
    private String responseJson;
    private String result;  // SUCCESS, FAIL
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
