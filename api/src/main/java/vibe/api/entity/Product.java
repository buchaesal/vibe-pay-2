package vibe.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 상품 Entity
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class Product {
    private String productNo;
    private String productName;
    private Integer price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
