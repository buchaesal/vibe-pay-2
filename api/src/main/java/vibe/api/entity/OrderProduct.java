package vibe.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 상품 Entity (스냅샷)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class OrderProduct {
    private String orderNo;
    private String productNo;
    private String productName;  // 주문 시점 상품명
    private Integer price;  // 주문 시점 판매가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
