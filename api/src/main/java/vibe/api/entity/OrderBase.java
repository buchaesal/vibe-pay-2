package vibe.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 기본 Entity
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@Setter
public class OrderBase {
    private String orderNo;
    private String memberNo;
    private LocalDateTime orderDatetime;
    private String ordererName;
    private String ordererPhone;
    private String ordererEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
