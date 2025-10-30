package vibepay.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vibepay.api.common.dto.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * 헬스체크 컨트롤러
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    /**
     * 헬스체크 API
     */
    @GetMapping
    public Response<Map<String, String>> healthCheck() {
        log.info("헬스체크 요청");

        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "vibepay-api");
        health.put("version", "1.0.0");

        return new Response<>(health);
    }
}
