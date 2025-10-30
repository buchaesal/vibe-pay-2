package vibepay.api.dto.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 회원 정보 응답 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class MemberResponse {
    private String memberNo;
    private String name;
    private String email;
    private Integer points;
}
