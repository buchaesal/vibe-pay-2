package vibepay.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 주문 취소 요청 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class OrderCancelRequest {
    @NotBlank
    private String orderNo;

    @NotNull
    private Integer orderSeq;

    @NotNull
    private Integer cancelQty;
}
