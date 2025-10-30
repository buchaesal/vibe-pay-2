package vibe.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vibe.api.common.dto.Response;
import vibe.api.dto.request.LoginRequest;
import vibe.api.dto.response.MemberResponse;
import vibe.api.service.MemberService;

/**
 * 인증 컨트롤러
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    /**
     * 로그인 API
     */
    @PostMapping("/login")
    public Response<MemberResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("로그인 요청: loginId={}", request.getLoginId());

        MemberResponse memberResponse = memberService.login(request);

        return new Response<>(memberResponse);
    }
}
