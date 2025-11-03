package vibe.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vibe.api.common.dto.Response;
import vibe.api.dto.request.MemberRegisterRequest;
import vibe.api.dto.response.MemberResponse;
import vibe.api.service.MemberService;

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

    /**
     * 회원정보 조회 API
     */
    @GetMapping("/{memberNo}")
    public Response<MemberResponse> getMember(@PathVariable String memberNo) {
        log.info("회원정보 조회 요청: memberNo={}", memberNo);

        MemberResponse memberResponse = memberService.getMember(memberNo);

        return new Response<>(memberResponse);
    }
}
