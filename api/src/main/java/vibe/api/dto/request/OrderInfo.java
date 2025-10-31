package vibe.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 주문 기본 정보 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-31
 */
@Getter
@Setter
public class OrderInfo {
    @NotBlank
    private String orderNo;

    @NotBlank
    private String memberNo;

    @NotBlank
    private String ordererName;

    @NotBlank
    private String ordererPhone;

    private String ordererEmail;

    @NotEmpty
    private List<Long> cartIdList;
}
