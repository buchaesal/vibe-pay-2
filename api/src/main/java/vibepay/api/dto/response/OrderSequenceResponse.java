package vibepay.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 주문번호 채번 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
@AllArgsConstructor
public class OrderSequenceResponse {
    private String orderNo;
}
