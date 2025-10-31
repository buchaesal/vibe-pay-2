package vibe.api.pg;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import vibe.api.common.config.PaymentProperties;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;
import vibe.api.service.PaymentInterfaceService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 이니시스 PG 클라이언트
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InicisClient {

    private final PaymentProperties paymentProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final PaymentInterfaceService paymentInterfaceService;

    /**
     * 인증 파라미터 생성 (주문서 화면용)
     */
    public Map<String, String> generateAuthParams(String orderNo, Integer price) {
        try {
            PaymentProperties.Inicis config = paymentProperties.getInicis();

            long timestamp = System.currentTimeMillis();
            String timestampStr = String.valueOf(timestamp);

            // signature: SHA256(oid + price + timestamp)
            String signatureData = String.format("oid=%s&price=%d&timestamp=%s", orderNo, price, timestampStr);
            String signature = sha256(signatureData);

            // verification: SHA256(oid + price + signKey + timestamp)
            String verificationData = String.format("oid=%s&price=%d&signKey=%s&timestamp=%s",
                orderNo, price, config.getSignKey(), timestampStr);
            String verification = sha256(verificationData);

            // mKey: SHA256(signKey)
            String mKey = sha256(config.getSignKey());

            return Map.of(
                "mid", config.getMid(),
                "timestamp", timestampStr,
                "mKey", mKey,
                "signature", signature,
                "verification", verification
            );
        } catch (Exception e) {
            log.error("이니시스 인증 파라미터 생성 실패", e);
            throw new ApiException(ErrorCode.PG_SIGN_ERROR);
        }
    }

    /**
     * 승인 요청
     */
    public Map<String, Object> approve(String orderNo, Long paymentNo, Map<String, Object> authResult) {
        Long interfaceSeq = null;

        try {
            PaymentProperties.Inicis config = paymentProperties.getInicis();

            String authToken = (String) authResult.get("authToken");
            String authUrl = (String) authResult.get("authUrl");

            long timestamp = System.currentTimeMillis();
            String timestampStr = String.valueOf(timestamp);

            // signature: SHA256(authToken + timestamp)
            String signatureData = String.format("authToken=%s&timestamp=%s", authToken, timestampStr);
            String signature = sha256(signatureData);

            // verification: SHA256(authToken + signKey + timestamp)
            String verificationData = String.format("authToken=%s&signKey=%s&timestamp=%s",
                authToken, config.getSignKey(), timestampStr);
            String verification = sha256(verificationData);

            // 승인 요청 파라미터
            Map<String, String> params = Map.of(
                "mid", config.getMid(),
                "authToken", authToken,
                "timestamp", timestampStr,
                "signature", signature,
                "verification", verification,
                "charset", "UTF-8",
                "format", "JSON"
            );

            // 1. 요청 로그 저장 (트랜잭션 분리)
            interfaceSeq = paymentInterfaceService.saveRequest("INICIS", "APPROVAL", orderNo, paymentNo, params);

            // 2. WebClient로 승인 요청
            WebClient webClient = webClientBuilder.baseUrl(authUrl).build();

            String response = webClient.post()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(buildFormData(params))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            // 3. 응답 로그 업데이트 (트랜잭션 분리) - 이니시스 resultCode 저장
            String resultCode = (String) result.get("resultCode");
            paymentInterfaceService.updateResponse(interfaceSeq, result, resultCode);

            // 승인 성공 확인
            if (!"0000".equals(resultCode)) {
                log.error("이니시스 승인 실패: {}", result);
                throw new ApiException(ErrorCode.APPROVE_FAIL);
            }

            log.info("이니시스 승인 성공: tid={}", result.get("tid"));
            return result;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("이니시스 승인 요청 실패", e);
            throw new ApiException(ErrorCode.APPROVE_FAIL);
        }
    }

    /**
     * 전체 취소
     */
    public void refund(String orderNo, Long paymentNo, String tid) {
        Long interfaceSeq = null;
        try {
            PaymentProperties.Inicis config = paymentProperties.getInicis();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            Map<String, Object> data = Map.of(
                "tid", tid,
                "msg", "단순변심"
            );

            String dataJson = objectMapper.writeValueAsString(data);

            // hashData: SHA512(apiKey + mid + type + timestamp + data)
            String hashInput = config.getApiKey() + config.getMid() + "refund" + timestamp + dataJson;
            String hashData = sha512(hashInput);

            Map<String, Object> requestBody = Map.of(
                "mid", config.getMid(),
                "type", "refund",
                "timestamp", timestamp,
                "clientIp", "127.0.0.1",
                "hashData", hashData,
                "data", data
            );

            // 1. 요청 로그 저장 (트랜잭션 분리)
            interfaceSeq = paymentInterfaceService.saveRequest("INICIS", "CANCEL", orderNo, paymentNo, requestBody);

            // 2. WebClient로 취소 요청
            WebClient webClient = webClientBuilder.baseUrl(config.getRefundUrl()).build();

            String response = webClient.post()
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            // 3. 응답 로그 업데이트 (트랜잭션 분리) - 이니시스 resultCode 저장
            String resultCode = (String) result.get("resultCode");
            paymentInterfaceService.updateResponse(interfaceSeq, result, resultCode);

            if (!"00".equals(resultCode)) {
                log.error("이니시스 취소 실패: {}", result);
                throw new ApiException(ErrorCode.CANCEL_FAIL);
            }

            log.info("이니시스 취소 성공: tid={}", tid);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("이니시스 취소 요청 실패", e);
            throw new ApiException(ErrorCode.CANCEL_FAIL);
        }
    }

    /**
     * 부분 취소
     */
    public void partialRefund(String orderNo, Long paymentNo, String tid, Integer cancelAmount, Integer confirmPrice) {
        Long interfaceSeq = null;
        try {
            PaymentProperties.Inicis config = paymentProperties.getInicis();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            Map<String, Object> data = Map.of(
                "tid", tid,
                "msg", "단순변심",
                "price", String.valueOf(cancelAmount),
                "confirmPrice", String.valueOf(confirmPrice)
            );

            String dataJson = objectMapper.writeValueAsString(data);

            // hashData: SHA512(apiKey + mid + type + timestamp + data)
            String hashInput = config.getApiKey() + config.getMid() + "partialRefund" + timestamp + dataJson;
            String hashData = sha512(hashInput);

            Map<String, Object> requestBody = Map.of(
                "mid", config.getMid(),
                "type", "partialRefund",
                "timestamp", timestamp,
                "clientIp", "127.0.0.1",
                "hashData", hashData,
                "data", data
            );

            // 1. 요청 로그 저장 (트랜잭션 분리)
            interfaceSeq = paymentInterfaceService.saveRequest("INICIS", "CANCEL", orderNo, paymentNo, requestBody);

            // 2. WebClient로 부분취소 요청
            WebClient webClient = webClientBuilder.baseUrl(config.getPartialRefundUrl()).build();

            String response = webClient.post()
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            // 3. 응답 로그 업데이트 (트랜잭션 분리) - 이니시스 resultCode 저장
            String resultCode = (String) result.get("resultCode");
            paymentInterfaceService.updateResponse(interfaceSeq, result, resultCode);

            if (!"00".equals(resultCode)) {
                log.error("이니시스 부분취소 실패: {}", result);
                throw new ApiException(ErrorCode.CANCEL_FAIL);
            }

            log.info("이니시스 부분취소 성공: tid={}, amount={}", tid, cancelAmount);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("이니시스 부분취소 요청 실패", e);
            throw new ApiException(ErrorCode.CANCEL_FAIL);
        }
    }

    /**
     * 망취소
     */
    public void netCancel(String orderNo, Long paymentNo, Map<String, Object> authResult) {
        Long interfaceSeq = null;
        try {
            PaymentProperties.Inicis config = paymentProperties.getInicis();

            String authToken = (String) authResult.get("authToken");
            String netCancelUrl = (String) authResult.get("netCancelUrl");

            long timestamp = System.currentTimeMillis();
            String timestampStr = String.valueOf(timestamp);

            // signature: SHA256(authToken + timestamp)
            String signatureData = String.format("authToken=%s&timestamp=%s", authToken, timestampStr);
            String signature = sha256(signatureData);

            // verification: SHA256(authToken + signKey + timestamp)
            String verificationData = String.format("authToken=%s&signKey=%s&timestamp=%s",
                authToken, config.getSignKey(), timestampStr);
            String verification = sha256(verificationData);

            Map<String, String> params = Map.of(
                "mid", config.getMid(),
                "authToken", authToken,
                "timestamp", timestampStr,
                "signature", signature,
                "verification", verification,
                "charset", "UTF-8",
                "format", "JSON"
            );

            // 1. 요청 로그 저장 (트랜잭션 분리)
            interfaceSeq = paymentInterfaceService.saveRequest("INICIS", "NET_CANCEL", orderNo, paymentNo, params);

            // 2. WebClient로 망취소 요청
            WebClient webClient = webClientBuilder.baseUrl(netCancelUrl).build();

            String response = webClient.post()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(buildFormData(params))
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> result = objectMapper.readValue(response, Map.class);

            // 3. 응답 로그 업데이트 (트랜잭션 분리) - 이니시스 resultCode 저장
            String resultCode = (String) result.get("resultCode");
            paymentInterfaceService.updateResponse(interfaceSeq, result, resultCode);

            if (!"0000".equals(resultCode)) {
                log.error("이니시스 망취소 실패: {}", result);
            } else {
                log.info("이니시스 망취소 성공");
            }

        } catch (Exception e) {
            log.error("이니시스 망취소 요청 실패 (무시)", e);
        }
    }

    // ===== Helper Methods =====

    private String sha256(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String sha512(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private String buildFormData(Map<String, String> params) {
        return params.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((a, b) -> a + "&" + b)
            .orElse("");
    }
}
