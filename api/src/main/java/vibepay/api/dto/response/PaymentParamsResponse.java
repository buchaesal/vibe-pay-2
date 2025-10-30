package vibepay.api.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 이니시스 인증 파라미터 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class PaymentParamsResponse {
    private String mid;
    private String timestamp;
    private String mKey;
    private String signature;
    private String verification;
}
