package vibe.api.common.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * API 공통 응답 객체
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 * @param <T> 응답 데이터 타입
 */
@Getter
public class Response<T> {
    private String timestamp;
    private String code;
    private String message;
    private T payload;

    /**
     * 성공 응답 생성자 (데이터 있음)
     */
    public Response(T payload) {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        this.code = "0000";
        this.message = "성공";
        this.payload = payload;
    }

    /**
     * 성공 응답 생성자 (데이터 없음)
     */
    public Response() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        this.code = "0000";
        this.message = "성공";
        this.payload = null;
    }

    /**
     * 에러 응답 생성자
     */
    public Response(String code, String message) {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        this.code = code;
        this.message = message;
        this.payload = null;
    }
}
