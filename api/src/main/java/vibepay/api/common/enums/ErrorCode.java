package vibepay.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 에러 코드 정의
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 성공
    SUCCESS("0000", "성공"),

    // 인증/인가 오류 (1xxx)
    LOGIN_FAIL("1001", "로그인 실패"),
    DUPLICATE_ID("1002", "아이디 중복 또는 필수 값 누락"),
    UNAUTHORIZED("1003", "인증되지 않은 사용자"),
    MEMBER_NOT_FOUND("1004", "회원을 찾을 수 없습니다"),

    // 상품 오류 (2xxx)
    PRODUCT_NOT_FOUND("2001", "상품을 찾을 수 없습니다"),
    DUPLICATE_PRODUCT("2002", "상품 등록 실패"),

    // 장바구니 오류 (3xxx)
    INVALID_CART_ID("3001", "장바구니 삭제 실패"),
    CART_NOT_FOUND("3002", "장바구니를 찾을 수 없습니다"),

    // 주문 오류 (4xxx)
    ORDER_NOT_FOUND("4001", "주문을 찾을 수 없습니다"),

    // 결제 오류 (5xxx)
    APPROVE_FAIL("5001", "결제 승인 실패"),
    CANCEL_FAIL("5002", "결제 취소 실패"),
    PG_SIGN_ERROR("5003", "PG 서명 오류"),
    PAYMENT_NOT_FOUND("5004", "환불 가능한 결제 내역을 찾을 수 없습니다"),

    // 서버 오류 (9xxx)
    INTERNAL_SERVER_ERROR("9000", "서버 오류가 발생했습니다"),
    VALIDATION_ERROR("9001", "입력값 검증 실패"),
    DATABASE_ERROR("9002", "데이터베이스 오류");

    private final String code;
    private final String message;
}
