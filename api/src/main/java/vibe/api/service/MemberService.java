package vibe.api.service;

import vibe.api.dto.request.LoginRequest;
import vibe.api.dto.request.MemberRegisterRequest;
import vibe.api.dto.response.MemberResponse;

/**
 * 회원 서비스 인터페이스
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
public interface MemberService {
    /**
     * 로그인
     */
    MemberResponse login(LoginRequest request);

    /**
     * 회원가입
     */
    void registerMember(MemberRegisterRequest request);
}
