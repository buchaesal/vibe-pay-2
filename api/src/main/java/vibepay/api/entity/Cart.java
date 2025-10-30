package vibepay.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 장바구니 Entity
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class Cart {
    private Long cartId;
    private String memberNo;
    private String productNo;
    private Integer qty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
