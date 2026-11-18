package com.flowspace.user.service.impl;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.entity.enums.Provider;
import com.flowspace.user.service.RefreshTokenVO;
import com.flowspace.user.service.UserVO;

// 사용자 DB 접근. 메서드 이름이 User_SQL.xml의 id와 같아야 해요.
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

    // 사용자 등록. 실행 후 DB가 만든 번호가 user의 userId에 들어가요.
    int insertUser(UserVO user);

    // 인증 전에 같은 이메일로 다시 가입할 때: 새로 입력한 닉네임과 비밀번호로 바꿔요
    int updateUserSignupRestart(@Param("userId") Long userId, @Param("nickname") String nickname,
        @Param("password") String password);

    // 이메일 인증 완료로 표시
    int updateUserEmailVerified(Long userId);

    // 인증 전 계정을 소셜 로그인으로 넘겨받을 때: 인증 완료로 바꾸고 가입 때 정한 비밀번호는 지워요
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
}
