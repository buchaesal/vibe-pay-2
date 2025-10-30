package vibe.api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 결제 PG 설정 Properties
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Configuration
@ConfigurationProperties(prefix = "payment")
@Getter
@Setter
public class PaymentProperties {
    private Inicis inicis;
    private Toss toss;

    @Getter
    @Setter
    public static class Inicis {
        private String mid;
        private String signKey;
        private String apiKey;
        private String authUrl;
        private String approvalUrlPrefix;
        private String refundUrl;
        private String partialRefundUrl;
    }

    @Getter
    @Setter
    public static class Toss {
        private String clientKey;
        private String secretKey;
        private String paymentUrl;
        private String apiBaseUrl;
    }
}
