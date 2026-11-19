package com.flowspace.user.service;

import com.flowspace.user.dto.request.GoogleLoginRequest;
import com.flowspace.user.dto.request.LoginRequest;
import com.flowspace.user.dto.request.MicrosoftLoginRequest;
import com.flowspace.user.dto.request.PasswordChangeRequest;
import com.flowspace.user.dto.request.ProfileUpdateRequest;
import com.flowspace.user.dto.request.RefreshRequest;
import com.flowspace.user.dto.request.SignupRequest;
import com.flowspace.user.dto.request.SocialLinkRequest;
import com.flowspace.user.dto.request.TokenRequest;
import com.flowspace.user.dto.response.LoginResponse;
import com.flowspace.user.dto.response.SignupResponse;
import com.flowspace.user.dto.response.TokenResponse;
import com.flowspace.user.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

// 인증 서비스
public interface AuthService {

    // 회원가입 (계정 생성 + 인증 메일 발송, 인증 전에는 토큰 미발급)
    SignupResponse signup(SignupRequest request);

    // 로그인 및 JWT 발급
    LoginResponse login(LoginRequest request);

    // 토큰 재발급 (저장된 refresh token이 일치하고 만료 전이면 새 access token 발급)
    LoginResponse refresh(RefreshRequest request);

    // Swagger OAuth2 로그인
    TokenResponse loginForSwagger(TokenRequest request);

    // 로그인 사용자 정보 조회
    UserResponse getMe(String email);

    // 프로필 이미지 수정
    UserResponse updateProfileImage(MultipartFile image, String email);

    // 프로필 수정
    UserResponse updateProfile(ProfileUpdateRequest request, MultipartFile image, String email);

    // 다른 기기 로그인 해제 (refresh token 재발급), 현재 기기는 새 토큰으로 유지
    LoginResponse changePassword(PasswordChangeRequest request, String email);

    // 프로필 이미지 삭제
    UserResponse deleteProfileImage(String email);

    // 소셜 토큰으로 이메일 소유, 비밀번호로 기존 계정 소유를 각각 확인
    LoginResponse linkSocialAccount(SocialLinkRequest request);

    // Google 로그인
    LoginResponse googleLogin(GoogleLoginRequest request);

    // Microsoft 로그인
    LoginResponse microsoftLogin(MicrosoftLoginRequest request);
}
