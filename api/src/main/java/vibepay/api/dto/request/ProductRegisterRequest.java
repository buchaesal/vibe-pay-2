package vibepay.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 상품 등록 요청 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class ProductRegisterRequest {
    @NotBlank(message = "상품명은 필수입니다")
    private String productName;

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다")
    private Integer price;
}
