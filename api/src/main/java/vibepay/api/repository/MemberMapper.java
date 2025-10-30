package vibepay.api.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vibepay.api.entity.Member;

import java.util.Optional;

/**
 * 회원 Mapper (조회 전용)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface MemberMapper {
    /**
     * 로그인 ID로 회원 조회
     */
    Optional<Member> selectMemberByLoginId(@Param("loginId") String loginId);

    /**
     * 회원번호로 회원 조회
     */
    Optional<Member> selectMemberByMemberNo(@Param("memberNo") String memberNo);

    /**
     * 로그인 ID 중복 확인
     */
    int countByLoginId(@Param("loginId") String loginId);

    /**
     * 회원번호 채번
     */
    String selectNextMemberNo();
}
