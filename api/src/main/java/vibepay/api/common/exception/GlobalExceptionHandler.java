package vibepay.api.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vibepay.api.common.dto.Response;
import vibepay.api.common.enums.ErrorCode;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리기
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ApiException 처리
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Response<Void>> handleApiException(ApiException e) {
        log.error("API 예외 발생: {}", e.getMessage(), e);

        Response<Void> response = new Response<>(
            e.getErrorCode().getCode(),
            e.getErrorCode().getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation 예외 발생: {}", e.getMessage(), e);

        String errorMessage = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        Response<Void> response = new Response<>(
            ErrorCode.VALIDATION_ERROR.getCode(),
            errorMessage
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception e) {
        log.error("예외 발생: {}", e.getMessage(), e);

        Response<Void> response = new Response<>(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
