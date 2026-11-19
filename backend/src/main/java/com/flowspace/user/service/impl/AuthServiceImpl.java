package com.flowspace.user.service.impl;

import com.flowspace.file.service.FileService;
import com.flowspace.file.service.FileVO;
import com.flowspace.user.service.EmailVerificationService;
import com.flowspace.user.service.AuthService;
import com.flowspace.user.service.RefreshTokenVO;
import com.flowspace.user.service.UserVO;

import com.flowspace.workspace.service.WorkspaceService;

import com.flowspace.user.dto.request.GoogleLoginRequest;
import com.flowspace.user.dto.social.GoogleUserInfo;
import com.flowspace.user.dto.request.LoginRequest;
import com.flowspace.user.dto.response.LoginResponse;
import com.flowspace.user.dto.request.MicrosoftLoginRequest;
import com.flowspace.user.dto.social.MicrosoftUserInfo;
import com.flowspace.user.dto.request.PasswordChangeRequest;
import com.flowspace.user.dto.request.ProfileUpdateRequest;
import com.flowspace.user.dto.request.RefreshRequest;
import com.flowspace.user.dto.request.SignupRequest;
import com.flowspace.user.dto.response.SignupResponse;
import com.flowspace.user.dto.request.SocialLinkRequest;
import com.flowspace.user.dto.response.UserResponse;
import com.flowspace.user.dto.response.TokenResponse;
import com.flowspace.user.dto.request.TokenRequest;
import com.flowspace.entity.enums.Provider;
import com.flowspace.exception.ErrorCode;
import com.flowspace.exception.FlowSpaceException;
import com.flowspace.security.JwtProvider;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final WorkspaceService workspaceService;
    private final FileService fileService;
    private final EmailVerificationService emailVerificationService;

    // 사용자 SQL
    private final UserMapper userMapper;

    // 로그인 상태를 유지하지 않을 때 리프레시 토큰이 살아 있는 시간
    private static final Duration SESSION_REFRESH_TTL = Duration.ofDays(1);

    @Value("${google.client-id}")
    private String googleClientId;
    @Value("${microsoft.client-id}")
    private String microsoftClientId;

    private static final String MICROSOFT_JWK_SET_URI = "https://login.microsoftonline.com/common/discovery/v2.0/keys";
    private static final String MICROSOFT_ISSUER_PREFIX = "https://login.microsoftonline.com/";

    private volatile JwtDecoder microsoftJwtDecoder;

    // 회원가입 (계정 생성 + 인증 메일 발송, 인증 전에는 토큰 미발급)
    @Transactional
    @Override
    public SignupResponse signup(SignupRequest request) {

        UserVO existing = userMapper.selectUserByEmail(request.email());

        if (existing != null) {

            if (Boolean.TRUE.equals(existing.getEmailVerified())) {
                throw new FlowSpaceException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }

            userMapper.updateUserSignupRestart(existing.getUserId(), request.nickname(),
                passwordEncoder.encode(request.password()));
            emailVerificationService.sendVerification(existing.getUserId(), existing.getEmail());

            return new SignupResponse(existing.getEmail());
        }

        UserVO user = new UserVO();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setProvider(Provider.LOCAL);
        user.setEmailVerified(false);

        userMapper.insertUser(user);

        workspaceService.createPersonalWorkspace(user.getUserId(), user.getNickname());

        emailVerificationService.sendVerification(user.getUserId(), user.getEmail());

        return new SignupResponse(user.getEmail());
    }

    // 로그인 및 JWT 발급
    @Override
    public LoginResponse login(LoginRequest request) {

        UserVO user = findLoginUser(request.email(), request.password());

        String accessToken = createAccessToken(user);
        String refreshToken = issueRefreshToken(user, request.remember());

        return LoginResponse.from(user, accessToken, refreshToken, user.getLastWorkspaceId());
    }

    // 토큰 재발급 (저장된 refresh token이 일치하고 만료 전이면 새 access token 발급)
    @Override
    public LoginResponse refresh(RefreshRequest request) {

        String token = request.refreshToken();

        if (!jwtProvider.isRefreshToken(token)) {
            throw new FlowSpaceException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshTokenVO saved = userMapper.selectRefreshToken(token);

        if (saved == null || saved.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new FlowSpaceException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserVO user = userMapper.selectUserById(saved.getUserId());

        if (user == null || user.getDeletedAt() != null) {
            throw new FlowSpaceException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return LoginResponse.from(user, createAccessToken(user), token, user.getLastWorkspaceId());
    }

    // Swagger OAuth2 로그인
    @Override
    public TokenResponse loginForSwagger(TokenRequest request) {

        UserVO user = findLoginUser(request.username(), request.password());

        return TokenResponse.of(createAccessToken(user), issueRefreshToken(user, true));
    }

    // 이메일·비밀번호 로그인 사용자 확인 (어느 쪽이 틀렸는지 구분하지 않음)
    private UserVO findLoginUser(String email, String password) {

        UserVO user = userMapper.selectUserByEmail(email);

        if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new FlowSpaceException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        return user;
    }

    // access token 발급 (번호·이메일·가입 방식 포함)
    private String createAccessToken(UserVO user) {
        return jwtProvider.createAccessToken(user.getUserId(), user.getEmail(), user.getProvider());
    }

    // 로그인 사용자 정보 조회
    @Transactional(readOnly = true)
    @Override
    public UserResponse getMe(String email) {
        return UserResponse.from(findUser(email));
    }

    // 프로필 이미지 수정
    @Transactional
    @Override
    public UserResponse updateProfileImage(MultipartFile image, String email) {

        UserVO user = findUser(email);

        FileVO file = fileService.uploadImage(image, profileWorkspaceId(user), email);

        userMapper.updateUserProfileFile(user.getUserId(), file.getFileId());

        return UserResponse.from(findUser(email));
    }

    // 프로필 수정
    @Override
    public UserResponse updateProfile(ProfileUpdateRequest request, MultipartFile image, String email) {

        UserVO user = findUser(email);

        if (image != null && !image.isEmpty()) {
            user.setProfileFileId(fileService.uploadImage(image, profileWorkspaceId(user), email).getFileId());
        }

        String bio = request.bio() == null ? null : request.bio().trim();

        user.setNickname(request.nickname().trim());
        user.setBio(bio == null || bio.isEmpty() ? null : bio);

        userMapper.updateUserProfile(user);

        return UserResponse.from(findUser(email));
    }

    // 비밀번호 변경
    // 다른 기기 로그인 해제 (refresh token 재발급), 현재 기기는 새 토큰으로 유지
    @Override
    public LoginResponse changePassword(PasswordChangeRequest request, String email) {

        UserVO user = findUser(email);

        if (user.getPassword() == null) {
            throw new FlowSpaceException(ErrorCode.PASSWORD_NOT_SET);
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new FlowSpaceException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new FlowSpaceException(ErrorCode.SAME_PASSWORD);
        }

        userMapper.updateUserPassword(user.getUserId(), passwordEncoder.encode(request.newPassword()));

        LocalDateTime expiredAt = userMapper.selectRefreshTokenExpiredAt(user.getUserId());

        Duration remaining = expiredAt == null ? SESSION_REFRESH_TTL : Duration.between(LocalDateTime.now(), expiredAt);

        if (remaining.isNegative()) {
            remaining = SESSION_REFRESH_TTL;
        }

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getEmail(), user.getProvider());
        String refreshToken = issueRefreshToken(user, remaining);

        return LoginResponse.from(user, accessToken, refreshToken, user.getLastWorkspaceId());
    }

    // 프로필 이미지 저장 워크스페이스: 마지막 워크스페이스, 없으면 첫 참여 워크스페이스
    private Long profileWorkspaceId(UserVO user) {

        return user.getLastWorkspaceId() != null ? user.getLastWorkspaceId()
            : userMapper.selectFirstWorkspaceId(user.getUserId());
    }

    // 이메일로 사용자 한 명 (없으면 예외)
    private UserVO findUser(String email) {

        UserVO user = userMapper.selectUserByEmail(email);

        if (user == null) {
            throw new FlowSpaceException(ErrorCode.USER_NOT_FOUND);
        }

        return user;
    }

    // refresh token 발급 (기존 토큰 삭제 후 저장, 계정당 하나)
    private String issueRefreshToken(UserVO user, Duration ttl) {

        String refreshToken = jwtProvider.createRefreshToken(user.getUserId(), ttl.toMillis());

        userMapper.deleteRefreshToken(user.getUserId());
        userMapper.insertRefreshToken(user.getUserId(), refreshToken, LocalDateTime.now().plus(ttl));

        return refreshToken;
    }

    // refresh token 발급 (로그인 유지면 설정 기간, 아니면 하루)
    private String issueRefreshToken(UserVO user, boolean remember) {

        Duration ttl = remember ? Duration.ofMillis(jwtProvider.getRefreshTokenExpiration()) : SESSION_REFRESH_TTL;

        return issueRefreshToken(user, ttl);
    }

    // 프로필 이미지 삭제
    @Override
    public UserResponse deleteProfileImage(String email) {

        UserVO user = findUser(email);

        if (user.getProfileFileId() != null) {
            userMapper.updateUserProfileFile(user.getUserId(), null);
            fileService.delete(user.getProfileFileId());
        }

        return UserResponse.from(findUser(email));
    }

    // 소셜 로그인 사용자 조회 (연결된 소셜 계정 우선, 없으면 users.provider 기준)
    private UserVO findSocialUser(Provider provider, String providerId) {

        Long userId = userMapper.selectSocialUserId(provider, providerId);

        if (userId == null) {
            userId = userMapper.selectUserIdByProvider(provider, providerId);
        }

        return userId == null ? null : userMapper.selectUserById(userId);
    }

    // 미인증 이메일 가입 계정을 소셜 계정 연결 + 인증 완료로 전환
    // 가입 시 비밀번호는 삭제 (이메일 주인이 아닌 사람이 먼저 가입했을 가능성)
    private UserVO claimUnverifiedAccount(String email, Provider provider, String providerId) {

        UserVO pending = userMapper.selectUserByEmail(email);

        if (pending == null || Boolean.TRUE.equals(pending.getEmailVerified()) || pending.getDeletedAt() != null) {
            return null;
        }

        userMapper.updateUserClaimBySocial(pending.getUserId());
        userMapper.insertSocialAccount(pending.getUserId(), provider, providerId);

        return userMapper.selectUserById(pending.getUserId());
    }

    // 같은 이메일 계정이 있을 때 새 계정 생성 차단
    // - 비밀번호가 있는 계정: 비밀번호 확인 후 연결 안내 (SOCIAL_LINK_REQUIRED)
    // 이메일만으로 합치면 계정 탈취 위험이 있어 비밀번호 확인 필수
    // - 그 외: 처음 가입한 방식으로 로그인 안내 (SOCIAL_EMAIL_CONFLICT)
    private void rejectExistingEmail(String email, Provider provider) {

        UserVO existing = userMapper.selectUserByEmail(email);

        if (existing == null) {
            return;
        }

        if (existing.getPassword() != null && existing.getDeletedAt() == null
            && userMapper.selectSocialAccountCount(existing.getUserId(), provider) == 0) {
            throw new FlowSpaceException(ErrorCode.SOCIAL_LINK_REQUIRED);
        }

        throw new FlowSpaceException(ErrorCode.SOCIAL_EMAIL_CONFLICT);
    }

    // 소셜 계정 연결 (비밀번호가 있는 기존 계정에 연결 후 로그인)
    // 소셜 토큰으로 이메일 소유, 비밀번호로 기존 계정 소유 확인
    @Override
    public LoginResponse linkSocialAccount(SocialLinkRequest request) {

        Provider provider = request.provider();
        String providerId;
        String email;

        if (provider == Provider.GOOGLE) {
            GoogleUserInfo info = verifyGoogleToken(request.idToken());
            providerId = info.providerId();
            email = info.email();
        } else if (provider == Provider.MICROSOFT) {
            MicrosoftUserInfo info = verifyMicrosoftToken(request.idToken());
            providerId = info.providerId();
            email = info.email();
        } else {
            throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
        }

        UserVO user = findSocialUser(provider, providerId);

        if (user == null) {

            UserVO existing = userMapper.selectUserByEmail(email);

            if (existing == null) {
                throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
            }

            if (existing.getDeletedAt() != null || existing.getPassword() == null
                || userMapper.selectSocialAccountCount(existing.getUserId(), provider) > 0) {
                throw new FlowSpaceException(ErrorCode.SOCIAL_EMAIL_CONFLICT);
            }

            if (!passwordEncoder.matches(request.password(), existing.getPassword())) {
                throw new FlowSpaceException(ErrorCode.SOCIAL_LINK_PASSWORD_MISMATCH);
            }

            userMapper.insertSocialAccount(existing.getUserId(), provider, providerId);
            userMapper.updateUserEmailVerified(existing.getUserId());

            user = userMapper.selectUserById(existing.getUserId());
        }

        return socialLoginResponse(user);
    }

    // 소셜 로그인 응답 (토큰 두 개 발급, 항상 로그인 유지)
    private LoginResponse socialLoginResponse(UserVO user) {
        return LoginResponse.from(user, createAccessToken(user), issueRefreshToken(user, true),
            user.getLastWorkspaceId());
    }

    // 소셜 회원 생성: 사용자 등록 → 소셜 계정 연결 → 개인 워크스페이스 (워크스페이스 번호 반환)
    private Long createSocialUser(UserVO user) {

        user.setPassword(null);
        user.setEmailVerified(true);

        userMapper.insertUser(user);
        userMapper.insertSocialAccount(user.getUserId(), user.getProvider(), user.getProviderId());

        return workspaceService.createPersonalWorkspace(user.getUserId(), user.getNickname());
    }

    // 소셜 프로필 사진 저장 후 사용자 프로필로 지정
    private void saveSocialProfileImage(UserVO user, Long workspaceId, byte[] imageBytes) {

        FileVO profile = fileService.uploadProfileImage(imageBytes, workspaceId, user.getUserId());

        userMapper.updateUserProfileFile(user.getUserId(), profile.getFileId());
    }

    // Google 로그인
    @Override
    public LoginResponse googleLogin(GoogleLoginRequest request) {

        GoogleUserInfo googleUser = verifyGoogleToken(request.idToken());

        UserVO user = findSocialUser(Provider.GOOGLE, googleUser.providerId());

        if (user == null) {
            user = createGoogleUser(googleUser);
        }

        return socialLoginResponse(user);
    }

    // Google 회원 생성
    private UserVO createGoogleUser(GoogleUserInfo googleUser) {

        UserVO pending = claimUnverifiedAccount(googleUser.email(), Provider.GOOGLE, googleUser.providerId());

        if (pending != null) {
            return pending;
        }

        rejectExistingEmail(googleUser.email(), Provider.GOOGLE);

        UserVO user = new UserVO();
        user.setEmail(googleUser.email());
        user.setNickname(googleUser.nickname());
        user.setProvider(Provider.GOOGLE);
        user.setProviderId(googleUser.providerId());

        Long workspaceId = createSocialUser(user);

        if (googleUser.pictureUrl() != null) {
            uploadGoogleProfile(user, workspaceId, googleUser.pictureUrl());
        }

        return userMapper.selectUserById(user.getUserId());
    }

    // Google 프로필 저장
    private void uploadGoogleProfile(UserVO user, Long workspaceId, String imageUrl) {

        try {

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);

            if (response.getBody() == null) {
                return;
            }

            saveSocialProfileImage(user, workspaceId, response.getBody());

        } catch (Exception ignored) {
        }
    }

    // Google ID Token 검증
    private GoogleUserInfo verifyGoogleToken(String idToken) {

        try {

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance()).setAudience(Collections.singletonList(googleClientId)).build();

            GoogleIdToken token = verifier.verify(idToken);

            if (token == null) {
                throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
            }

            GoogleIdToken.Payload payload = token.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new FlowSpaceException(ErrorCode.SOCIAL_EMAIL_NOT_VERIFIED);
            }

            return new GoogleUserInfo(payload.getSubject(), payload.getEmail(), (String) payload.get("name"),
                (String) payload.get("picture"));

        } catch (FlowSpaceException e) {
            throw e;
        } catch (Exception e) {
            throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
        }
    }

    // Microsoft 로그인
    @Override
    public LoginResponse microsoftLogin(MicrosoftLoginRequest request) {

        MicrosoftUserInfo microsoftUser = verifyMicrosoftToken(request.idToken());

        UserVO user = findSocialUser(Provider.MICROSOFT, microsoftUser.providerId());

        if (user == null) {
            user = createMicrosoftUser(microsoftUser, request.accessToken());
        }

        return socialLoginResponse(user);
    }

    // Microsoft 회원 생성
    private UserVO createMicrosoftUser(MicrosoftUserInfo microsoftUser, String accessToken) {

        UserVO pending = claimUnverifiedAccount(microsoftUser.email(), Provider.MICROSOFT, microsoftUser.providerId());

        if (pending != null) {
            return pending;
        }

        rejectExistingEmail(microsoftUser.email(), Provider.MICROSOFT);

        UserVO user = new UserVO();
        user.setEmail(microsoftUser.email());
        user.setNickname(microsoftUser.nickname());
        user.setProvider(Provider.MICROSOFT);
        user.setProviderId(microsoftUser.providerId());

        Long workspaceId = createSocialUser(user);

        uploadMicrosoftProfile(user, workspaceId, accessToken);

        return userMapper.selectUserById(user.getUserId());
    }

    // Microsoft 프로필 저장
    private void uploadMicrosoftProfile(UserVO user, Long workspaceId, String accessToken) {

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange("https://graph.microsoft.com/v1.0/me/photo/$value",
                HttpMethod.GET, entity, byte[].class);

            if (response.getBody() == null) {
                return;
            }

            saveSocialProfileImage(user, workspaceId, response.getBody());

        } catch (Exception ignored) {
        }
    }

    // Microsoft ID Token 검증
    // - 발급처·서명 키가 달라 Microsoft 공개 서명 키로 직접 확인
    // - 서명·만료는 디코더, client-id(aud)·발급처(iss)는 아래에서 확인
    private MicrosoftUserInfo verifyMicrosoftToken(String idToken) {

        try {

            Jwt jwt = microsoftDecoder().decode(idToken);

            List<String> audience = jwt.getAudience();
            String issuer = jwt.getClaimAsString("iss");

            if (audience == null || !audience.contains(microsoftClientId) || issuer == null
                || !issuer.startsWith(MICROSOFT_ISSUER_PREFIX) || !issuer.endsWith("/v2.0")) {
                throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
            }

            String email = jwt.getClaimAsString("email");

            if (email == null || email.isBlank()) {
                email = jwt.getClaimAsString("preferred_username");
            }

            if (email == null || email.isBlank() || !email.contains("@")) {
                throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
            }

            String name = jwt.getClaimAsString("name");

            if (name == null || name.isBlank()) {
                name = email.substring(0, email.indexOf('@'));
            }

            return new MicrosoftUserInfo(jwt.getSubject(), email, name);

        } catch (FlowSpaceException e) {
            throw e;
        } catch (Exception e) {
            throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
        }
    }

    // Microsoft 공개 서명 키(JWKS) 기반 ID Token 디코더 (최초 사용 시 생성, 키는 자동 캐시)
    private JwtDecoder microsoftDecoder() {

        JwtDecoder decoder = microsoftJwtDecoder;

        if (decoder == null) {
            decoder = NimbusJwtDecoder.withJwkSetUri(MICROSOFT_JWK_SET_URI).build();
            microsoftJwtDecoder = decoder;
        }

        return decoder;
    }
}
