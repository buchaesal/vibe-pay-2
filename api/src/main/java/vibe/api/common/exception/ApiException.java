package vibe.api.common.exception;

import lombok.Getter;
import vibe.api.common.enums.ErrorCode;

/**
 * API 예외 클래스
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
