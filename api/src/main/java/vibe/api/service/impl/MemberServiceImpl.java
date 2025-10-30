package vibe.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vibe.api.common.enums.ErrorCode;
import vibe.api.common.exception.ApiException;
import vibe.api.dto.request.LoginRequest;
import vibe.api.dto.request.MemberRegisterRequest;
import vibe.api.dto.response.MemberResponse;
import vibe.api.entity.Member;
import vibe.api.repository.MemberMapper;
import vibe.api.repository.MemberTrxMapper;
import vibe.api.service.MemberService;

/**
 * 회원 서비스 구현
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final MemberTrxMapper memberTrxMapper;

    /**
     * 로그인
     * JWT 없이 간단하게 구현 (비밀번호 평문 비교)
     */
    @Override
    public MemberResponse login(LoginRequest request) {
        log.debug("로그인 시도: loginId={}", request.getLoginId());

        // 회원 조회
        Member member = memberMapper.selectMemberByLoginId(request.getLoginId())
            .orElseThrow(() -> new ApiException(ErrorCode.LOGIN_FAIL));

        // 비밀번호 검증 (평문 비교 - 실무에서는 BCrypt 등 사용)
        if (!member.getPassword().equals(request.getPassword())) {
            throw new ApiException(ErrorCode.LOGIN_FAIL);
        }

        log.info("로그인 성공: memberNo={}, name={}", member.getMemberNo(), member.getName());

        // 응답 DTO 생성
        MemberResponse response = new MemberResponse();
        response.setMemberNo(member.getMemberNo());
        response.setName(member.getName());
        response.setEmail(member.getEmail());
        response.setPhone(member.getPhone());
        response.setPoints(member.getPoints());

        return response;
    }

    /**
     * 회원가입
     */
    @Override
    @Transactional
    public void registerMember(MemberRegisterRequest request) {
        log.debug("회원가입 시도: loginId={}", request.getLoginId());

        // 중복 확인
        int count = memberMapper.countByLoginId(request.getLoginId());
        if (count > 0) {
            throw new ApiException(ErrorCode.DUPLICATE_ID);
        }

        // 회원번호 채번
        String memberNo = memberMapper.selectNextMemberNo();

        // Entity 생성
        Member member = new Member();
        member.setMemberNo(memberNo);
        member.setLoginId(request.getLoginId());
        member.setPassword(request.getPassword()); // 실무에서는 암호화 필요
        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setPhone(request.getPhone());
        member.setPoints(0); // 초기 적립금 0원

        // 회원 등록
        int result = memberTrxMapper.insertMember(member);
        if (result != 1) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        log.info("회원가입 성공: memberNo={}, loginId={}", memberNo, request.getLoginId());
    }
}
