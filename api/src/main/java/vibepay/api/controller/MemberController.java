package vibepay.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vibepay.api.common.dto.Response;
import vibepay.api.dto.request.MemberRegisterRequest;
import vibepay.api.service.MemberService;

/**
 * 회원 컨트롤러
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입 API
     */
    @PostMapping
    public Response<Void> registerMember(@RequestBody @Valid MemberRegisterRequest request) {
        log.info("회원가입 요청: loginId={}", request.getLoginId());

        memberService.registerMember(request);

        return new Response<>();
    }
}
