package vibe.api.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 상품 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class ProductResponse {
    private String productNo;
    private String productName;
    private Integer price;
}
