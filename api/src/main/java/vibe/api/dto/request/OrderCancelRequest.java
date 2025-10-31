package vibe.api.dto.request;

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

    // 부분취소 시 필수
    private Integer orderSeq;

    // 부분취소 시 필수
    private Integer cancelQty;

    // 전체취소 여부 (true면 orderSeq, cancelQty 무시)
    private Boolean isFullCancel;
}
