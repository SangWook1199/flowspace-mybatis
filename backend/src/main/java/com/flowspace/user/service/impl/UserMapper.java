package com.flowspace.user.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.common.enums.Provider;
import com.flowspace.user.service.MailTokenVO;
import com.flowspace.user.service.RefreshTokenVO;
import com.flowspace.user.service.UserLookupVO;
import com.flowspace.user.service.UserVO;
import com.flowspace.user.service.WithdrawWorkspaceVO;

// 사용자 Mapper (User_SQL.xml)
@Mapper
public interface UserMapper {

    // 이메일로 사용자 한 명 (프로시저 sp_user_by_email, 없으면 null)
    UserVO selectUserByEmail(String email);

    // 프로필 수정 (닉네임·한 줄 소개·프로필 이미지)
    int updateUserProfile(UserVO user);

    // 프로필 이미지만 바꾸기 (null이면 이미지 없음)
    int updateUserProfileFile(@Param("userId") Long userId, @Param("profileFileId") Long profileFileId);

    // 비밀번호 바꾸기 (암호화된 값)
    int updateUserPassword(@Param("userId") Long userId, @Param("password") String password);

    // 참여 중인 첫 워크스페이스 번호 (없으면 null)
    Long selectFirstWorkspaceId(Long userId);

    // 리프레시 토큰의 만료 시각 (없으면 null, 계정당 하나)
    LocalDateTime selectRefreshTokenExpiredAt(Long userId);

    // 리프레시 토큰 지우기
    int deleteRefreshToken(Long userId);

    // 리프레시 토큰 저장
    int insertRefreshToken(@Param("userId") Long userId, @Param("token") String token,
        @Param("expiredAt") LocalDateTime expiredAt);

    // 번호로 사용자 한 명 (프로시저 sp_user, 없으면 null)
    UserVO selectUserById(Long userId);

    // 사용자 등록 (생성된 번호는 userId에 설정)
    int insertUser(UserVO user);

    // 인증 전 같은 이메일로 재가입 시 닉네임·비밀번호 변경
    int updateUserSignupRestart(@Param("userId") Long userId, @Param("nickname") String nickname,
        @Param("password") String password);

    // 이메일 인증 완료로 표시
    int updateUserEmailVerified(Long userId);

    // 인증 전 계정을 소셜 로그인으로 전환 (인증 완료, 가입 시 비밀번호 삭제)
    int updateUserClaimBySocial(Long userId);

    // 저장된 리프레시 토큰 (없으면 null)
    RefreshTokenVO selectRefreshToken(String token);

    // 소셜 계정에 연결된 사용자 번호 (없으면 null)
    Long selectSocialUserId(@Param("provider") Provider provider, @Param("providerId") String providerId);

    // 예전 방식(users.provider)으로 가입한 소셜 사용자 번호 (없으면 null)
    Long selectUserIdByProvider(@Param("provider") Provider provider, @Param("providerId") String providerId);

    // 이 사용자에게 이 소셜 계정이 이미 연결돼 있는지 (0이면 없음)
    int selectSocialAccountCount(@Param("userId") Long userId, @Param("provider") Provider provider);

    // 소셜 계정 연결
    int insertSocialAccount(@Param("userId") Long userId, @Param("provider") Provider provider,
        @Param("providerId") String providerId);

    // ───── 이메일 인증 토큰 (email_verification_tokens) ─────

    // 가장 최근에 보낸 인증 메일 시각 (보낸 적 없으면 null)
    LocalDateTime selectLastEmailTokenCreatedAt(Long userId);

    // 사용자의 인증 토큰 모두 지우기
    int deleteEmailTokens(Long userId);

    // 인증 토큰 저장 (해시만)
    int insertEmailToken(@Param("userId") Long userId, @Param("tokenHash") String tokenHash,
        @Param("expiresAt") LocalDateTime expiresAt);

    // 해시로 인증 토큰 찾기 (없으면 null)
    MailTokenVO selectEmailTokenByHash(String tokenHash);

    // ───── 비밀번호 재설정 토큰 (password_reset_tokens) ─────

    // 가장 최근에 보낸 재설정 메일 시각 (보낸 적 없으면 null)
    LocalDateTime selectLastPasswordTokenCreatedAt(Long userId);

    // 사용자의 재설정 토큰 모두 지우기
    int deletePasswordTokens(Long userId);

    // 재설정 토큰 한 개 지우기 (만료된 링크를 눌렀을 때)
    int deletePasswordTokenByHash(String tokenHash);

    // 재설정 토큰 저장 (해시만)
    int insertPasswordToken(@Param("userId") Long userId, @Param("tokenHash") String tokenHash,
        @Param("expiresAt") LocalDateTime expiresAt);

    // 해시로 재설정 토큰 찾기 (없으면 null)
    MailTokenVO selectPasswordTokenByHash(String tokenHash);

    // ───── 탈퇴·사용자 찾기 ─────

    // 내가 속한 워크스페이스마다 내 역할과 멤버 수 (프로시저 sp_withdraw_check)
    List<WithdrawWorkspaceVO> selectWithdrawCheckList(Long userId);

    // 회원 탈퇴 처리 (프로시저 sp_withdraw_user)
    void updateUserWithdraw(@Param("userId") Long userId, @Param("nickname") String nickname);

    // 이메일로 사용자 한 명과 그 워크스페이스의 멤버·초대 여부 (프로시저 sp_user_lookup, 없으면 null)
    UserLookupVO selectUserLookup(@Param("email") String email, @Param("workspaceId") Long workspaceId);
}
