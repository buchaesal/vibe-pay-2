package vibe.api.pg;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import vibe.api.common.config.PaymentProperties;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 PG 클라이언트
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossClient {

    private final PaymentProperties paymentProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 승인 요청
     */
    public Map<String, Object> approve(Map<String, Object> authResult) {
        try {
            PaymentProperties.Toss config = paymentProperties.getToss();

            String paymentKey = (String) authResult.get("paymentKey");
            String orderId = (String) authResult.get("orderId");
            Integer amount = (Integer) authResult.get("amount");

            // Basic Auth 인증 헤더 생성
            String auth = config.getSecretKey() + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            // 승인 요청 파라미터
            Map<String, Object> requestBody = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
            );

            WebClient webClient = webClientBuilder
                .baseUrl(config.getApiBaseUrl())
                .build();

            String response = webClient.post()
                .uri("/payments/confirm")
                .header("Authorization", "Basic " + encodedAuth)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            // 승인 성공 확인 (status = DONE)
            if (!"DONE".equals(result.get("status"))) {
                log.error("토스 승인 실패: {}", result);
                throw new ApiException(ErrorCode.APPROVE_FAIL);
            }

            log.info("토스 승인 성공: paymentKey={}", paymentKey);
            return result;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("토스 승인 요청 실패", e);
            throw new ApiException(ErrorCode.APPROVE_FAIL);
        }
    }

    /**
     * 취소 (전체/부분 모두 동일 API)
     */
    public void refund(String paymentKey, Integer cancelAmount) {
        try {
            PaymentProperties.Toss config = paymentProperties.getToss();

            // Basic Auth 인증 헤더 생성
            String auth = config.getSecretKey() + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            // 취소 요청 파라미터
            Map<String, Object> requestBody = Map.of(
                "cancelReason", "단순변심",
                "cancelAmount", cancelAmount
            );

            WebClient webClient = webClientBuilder
                .baseUrl(config.getApiBaseUrl())
                .build();

            String response = webClient.post()
                .uri("/payments/" + paymentKey + "/cancel")
                .header("Authorization", "Basic " + encodedAuth)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            // 취소 성공 확인 (status = CANCELED)
            if (!"CANCELED".equals(result.get("status"))) {
                log.error("토스 취소 실패: {}", result);
                throw new ApiException(ErrorCode.CANCEL_FAIL);
            }

            log.info("토스 취소 성공: paymentKey={}, amount={}", paymentKey, cancelAmount);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("토스 취소 요청 실패", e);
            throw new ApiException(ErrorCode.CANCEL_FAIL);
        }
    }

    /**
     * 망취소 (토스는 별도 API 없음 - 일반 취소 재사용)
     */
    public void netCancel(Map<String, Object> authResult) {
        try {
            String paymentKey = (String) authResult.get("paymentKey");
            Integer amount = (Integer) authResult.get("amount");

            // 일반 취소 API 재사용
            refund(paymentKey, amount);

            log.info("토스 망취소 성공: paymentKey={}", paymentKey);

        } catch (Exception e) {
            log.error("토스 망취소 요청 실패 (무시)", e);
        }
    }
}
