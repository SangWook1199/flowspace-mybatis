package com.flowspace.user.service.impl;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
