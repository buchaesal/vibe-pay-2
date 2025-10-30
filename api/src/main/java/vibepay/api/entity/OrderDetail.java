package vibepay.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 상세 Entity
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class OrderDetail {
    private String orderNo;
    private Integer orderSeq;
    private Integer processSeq;
    private Integer parentProcessSeq;
    private String claimNo;
    private String productNo;
    private String orderType;  // ORDER, CANCEL
    private LocalDateTime orderDatetime;
    private LocalDateTime completeDatetime;
    private Integer orderQty;
    private Integer cancelQty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
