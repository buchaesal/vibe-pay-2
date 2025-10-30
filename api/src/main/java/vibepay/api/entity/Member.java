package vibepay.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 회원 Entity
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class Member {
    private String memberNo;
    private String loginId;
    private String name;
    private String password;
    private String email;
    private String phone;
    private Integer points;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
