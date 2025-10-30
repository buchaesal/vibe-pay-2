package vibe.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 장바구니 담기 요청 DTO
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class AddToCartRequest {
    @NotBlank(message = "회원번호는 필수입니다")
    private String memberNo;

    @NotEmpty(message = "상품 목록은 필수입니다")
    private List<String> productNoList;
}
