package com.flowspace.user.web;

import com.flowspace.user.dto.request.EmailResendRequest;
import com.flowspace.user.dto.request.EmailVerifyRequest;
import com.flowspace.user.dto.request.GoogleLoginRequest;
import com.flowspace.user.dto.request.LoginRequest;
import com.flowspace.user.dto.response.LoginResponse;
import com.flowspace.user.dto.request.MicrosoftLoginRequest;
import com.flowspace.user.dto.request.PasswordChangeRequest;
import com.flowspace.user.dto.request.PasswordForgotRequest;
import com.flowspace.user.dto.request.PasswordResetRequest;
import com.flowspace.user.dto.request.SocialLinkRequest;
import com.flowspace.user.dto.request.ProfileUpdateRequest;
import com.flowspace.user.dto.request.RefreshRequest;
import com.flowspace.user.dto.request.SignupRequest;
import com.flowspace.user.dto.response.SignupResponse;
import com.flowspace.user.dto.response.UserResponse;
import com.flowspace.user.dto.response.WithdrawCheckResponse;
import com.flowspace.user.dto.request.WithdrawRequest;
import com.flowspace.user.dto.response.TokenResponse;
import com.flowspace.user.dto.request.TokenRequest;
import com.flowspace.user.service.AccountService;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.security.AuthRateLimiter;
import com.flowspace.user.service.AuthService;
import com.flowspace.user.service.EmailVerificationService;
import com.flowspace.user.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final AuthRateLimiter authRateLimiter;

    @Operation(summary = "회원가입", description = "계정을 만들고 인증 메일을 보냅니다. 메일의 링크로 인증해야 로그인할 수 있습니다.")
    @PostMapping("/signup")
    public SignupResponse signup(@Valid @RequestBody SignupRequest request, HttpServletRequest http) {
        authRateLimiter.checkMailAllowed(http.getRemoteAddr(), request.email());

        return authService.signup(request);
    }

    @Operation(summary = "로그인", description = "성공 시 accessToken과 refreshToken을 반환합니다. 같은 이메일로 15분 안에 5번 틀리면 잠시 막습니다(429).")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String ip = http.getRemoteAddr();

        authRateLimiter.checkLoginAllowed(ip, request.email());

        try {
            LoginResponse response = authService.login(request);
            authRateLimiter.clearLoginFailures(ip, request.email());

            return response;
        } catch (FlowSpaceException e) {
            if (e.getErrorCode() == ErrorCode.INVALID_LOGIN) {
                authRateLimiter.recordLoginFailure(ip, request.email());
            }
            throw e;
        }
    }

    @Operation(summary = "토큰 재발급", description = "refreshToken으로 새 accessToken을 발급합니다.")
    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public TokenResponse token(@Valid @ModelAttribute TokenRequest request) {
        return authService.loginForSwagger(request);
    }

    @Operation(summary = "내 정보 조회")
    @SecurityRequirement(name = "OAuth2")
    @GetMapping("/me")
    public UserResponse getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return authService.getMe(userDetails.getUsername());
    }

    @Operation(summary = "프로필 이미지 수정")
    @PatchMapping(value = "/me/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateProfileImage(@RequestPart MultipartFile image,
        @AuthenticationPrincipal UserDetails userDetails) {

        return authService.updateProfileImage(image, userDetails.getUsername());
    }

    @Operation(summary = "프로필 수정")
    @PatchMapping(value = "/me/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateProfile(@RequestPart("data") @Valid ProfileUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image,
        @AuthenticationPrincipal UserDetails userDetails) {

        return authService.updateProfile(request, image, userDetails.getUsername());
    }

    @Operation(summary = "비밀번호 변경", description = "성공하면 다른 기기의 로그인은 풀리고, 이 기기용 새 토큰을 반환합니다.")
    @PutMapping("/me/password")
    public LoginResponse changePassword(@Valid @RequestBody PasswordChangeRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {

        return authService.changePassword(request, userDetails.getUsername());
    }

    @Operation(summary = "회원 탈퇴 사전 확인", description = "탈퇴하면 내 워크스페이스가 어떻게 되는지(탈퇴 가능 여부 포함)를 반환합니다.")
    @GetMapping("/me/withdraw-check")
    public WithdrawCheckResponse checkWithdraw(@AuthenticationPrincipal UserDetails userDetails) {

        return accountService.checkWithdraw(userDetails.getUsername());
    }

    @Operation(summary = "회원 탈퇴", description = "개인정보를 지우고 '탈퇴한 사용자'로 익명 처리합니다. 이메일 계정은 비밀번호, 소셜 계정은 이메일 입력으로 확인합니다.")
    @PostMapping("/me/withdraw")
    public void withdraw(@Valid @RequestBody WithdrawRequest request, @AuthenticationPrincipal UserDetails userDetails) {

        accountService.withdraw(request, userDetails.getUsername());
    }

    @Operation(summary = "프로필 이미지 삭제")
    @DeleteMapping("/me/profile/image")
    public UserResponse deleteProfileImage(@AuthenticationPrincipal UserDetails userDetails) {

        return authService.deleteProfileImage(userDetails.getUsername());
    }

    @Operation(summary = "Google 로그인")
    @PostMapping("/google")
    public LoginResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.googleLogin(request);
    }

    // Microsoft 로그인
    @Operation(summary = "Microsoft 로그인")
    @PostMapping("/microsoft")
    public LoginResponse microsoftLogin(@Valid @RequestBody MicrosoftLoginRequest request) {
        return authService.microsoftLogin(request);
    }

    @Operation(summary = "소셜 계정 연결", description = "같은 이메일의 기존 계정(이메일 가입)의 비밀번호로 확인하고 Google/Microsoft 계정을 연결한 뒤 로그인합니다.")
    @PostMapping("/social/link")
    public LoginResponse linkSocialAccount(@Valid @RequestBody SocialLinkRequest request) {
        return authService.linkSocialAccount(request);
    }

    @Operation(summary = "이메일 인증", description = "메일 링크의 토큰으로 가입 이메일을 인증합니다.")
    @PostMapping("/email/verify")
    public void verifyEmail(@Valid @RequestBody EmailVerifyRequest request) {
        emailVerificationService.verify(request);
    }

    @Operation(summary = "인증 메일 다시 보내기", description = "인증하지 않은 계정이면 인증 링크를 다시 보냅니다. 가입 여부와 관계없이 항상 성공으로 응답합니다.")
    @PostMapping("/email/resend")
    public void resendVerification(@Valid @RequestBody EmailResendRequest request, HttpServletRequest http) {
        authRateLimiter.checkMailAllowed(http.getRemoteAddr(), request.email());

        emailVerificationService.resend(request);
    }

    @Operation(summary = "비밀번호 찾기 메일 발송", description = "가입된 이메일이면 재설정 링크를 보냅니다. 가입 여부와 관계없이 항상 성공으로 응답합니다.")
    @PostMapping("/password/forgot")
    public void forgotPassword(@Valid @RequestBody PasswordForgotRequest request, HttpServletRequest http) {
        authRateLimiter.checkMailAllowed(http.getRemoteAddr(), request.email());

        passwordResetService.requestReset(request);
    }

    @Operation(summary = "비밀번호 재설정", description = "메일 링크의 토큰으로 새 비밀번호를 정합니다. 성공하면 모든 기기의 로그인이 풀립니다.")
    @PostMapping("/password/reset")
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.reset(request);
    }
}
