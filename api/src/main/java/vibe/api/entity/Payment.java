package vibe.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 결제 Entity
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class Payment {
    private Long paymentNo;
    private String orderNo;
    private String paymentType;  // PAYMENT, REFUND
    private String pgType;  // INICIS, TOSS
    private String paymentMethod;  // CARD, POINT
    private Integer paymentAmount;
    private String claimNo;
    private Integer remainRefundableAmount;
    private LocalDateTime paymentDatetime;
    private String approvalNo;
    private String tid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
