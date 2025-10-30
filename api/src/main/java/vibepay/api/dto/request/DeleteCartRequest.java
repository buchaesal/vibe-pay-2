package vibepay.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 장바구니 삭제 요청 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class DeleteCartRequest {
    @NotEmpty(message = "장바구니 ID 목록은 필수입니다")
    private List<Long> cartIdList;
}
