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

// AuthServiceImpl가 실제 동작을 맡아요. Controller와 다른 서비스는 이 인터페이스만 알아요.
public interface AuthService {

    // 회원가입: 계정을 만들고 인증 메일을 보내요. 메일의 링크로 인증해야 로그인할 수 있어서 토큰은 아직 주지 않아요.
    SignupResponse signup(SignupRequest request);

    // 로그인 및 JWT 발급
    LoginResponse login(LoginRequest request);

    // 토큰 재발급 (로그인 때 저장해 둔 refresh token이 맞고 만료 전이면 새 access token을 발급해요)
    LoginResponse refresh(RefreshRequest request);

    // Swagger OAuth2 로그인
    TokenResponse loginForSwagger(TokenRequest request);

    // 로그인 사용자 정보 조회
    UserResponse getMe(String email);

    // 프로필 이미지 수정
    UserResponse updateProfileImage(MultipartFile image, String email);

    // 프로필 수정
    UserResponse updateProfile(ProfileUpdateRequest request, MultipartFile image, String email);

    // 다른 기기의 로그인은 풀리고(리프레시 토큰을 새로 발급), 지금 쓰는 기기는 새 토큰을 받아 그대로 이어가요.
    LoginResponse changePassword(PasswordChangeRequest request, String email);

    // 프로필 이미지 삭제
    UserResponse deleteProfileImage(String email);

    // 소셜 토큰이 그 이메일의 주인임을, 비밀번호가 기존 계정의 주인임을 각각 증명해요.
    LoginResponse linkSocialAccount(SocialLinkRequest request);

    // Google 로그인
    LoginResponse googleLogin(GoogleLoginRequest request);

    // Microsoft 로그인
    LoginResponse microsoftLogin(MicrosoftLoginRequest request);
}
