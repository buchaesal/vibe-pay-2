package vibepay.api.repository;

import org.apache.ibatis.annotations.Mapper;
import vibepay.api.entity.Member;

/**
 * 회원 TrxMapper (등록/수정/삭제)
 *
 * @author Claude
 * @version 1.0
 * @since 2025-10-30
 */
@Mapper
public interface MemberTrxMapper {
    /**
     * 회원 등록
     */
    int insertMember(Member member);

    /**
     * 회원 정보 수정
     */
    int updateMember(Member member);

    /**
     * 적립금 차감
     */
    int updatePoints(Member member);

    /**
     * 회원 적립금 업데이트 (amount만큼 증감)
     */
    void updateMemberPoints(@org.apache.ibatis.annotations.Param("memberNo") String memberNo,
                            @org.apache.ibatis.annotations.Param("amount") Integer amount);
}
