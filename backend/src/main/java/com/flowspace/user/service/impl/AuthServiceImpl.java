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

    // 사용자 조회·수정용 MyBatis Mapper (같은 패키지라 import가 없어요)
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

    // 회원가입: 계정을 만들고 인증 메일을 보내요. 메일의 링크로 인증해야 로그인할 수 있어서 토큰은 아직 주지 않아요.
    @Transactional
    @Override
    public SignupResponse signup(SignupRequest request) {

        UserVO existing = userMapper.selectUserByEmail(request.email());

        if (existing != null) {

            // 이미 인증을 마친 계정이 있으면 가입할 수 없어요.
            if (Boolean.TRUE.equals(existing.getEmailVerified())) {
                throw new FlowSpaceException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }

            // 인증하지 않은 채 남은 계정이면 같은 이메일로 다시 가입하는 걸로 보고, 새로 입력한 정보로 바꿔서 인증 메일을 다시 보내요.
            // (인증 전에는 로그인할 수 없어서, 남의 이메일로 먼저 가입해 두어도 그 주소의 주인이 다시 가입하면 되돌릴 수 있어요.)
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

        // INSERT가 끝나면 DB가 만든 번호가 user.getUserId()에 들어 있어요
        userMapper.insertUser(user);

        // 개인 워크스페이스 자동 생성
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

    // 토큰 재발급 (로그인 때 저장해 둔 refresh token이 맞고 만료 전이면 새 access token을 발급해요)
    @Override
    public LoginResponse refresh(RefreshRequest request) {

        String token = request.refreshToken();

        // 서명이 맞는 refresh token만 받아요(access token을 넣어도 재발급되지 않아요).
        if (!jwtProvider.isRefreshToken(token)) {
            throw new FlowSpaceException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshTokenVO saved = userMapper.selectRefreshToken(token);

        if (saved == null || saved.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new FlowSpaceException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserVO user = userMapper.selectUserById(saved.getUserId());

        // 탈퇴한 계정은 재발급하지 않아요.
        if (user == null || user.getDeletedAt() != null) {
            throw new FlowSpaceException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 동시에 여러 탭이 재발급을 요청해도 서로 끊기지 않도록 refresh token은 그대로 돌려줘요.
        return LoginResponse.from(user, createAccessToken(user), token, user.getLastWorkspaceId());
    }

    // Swagger OAuth2 로그인
    @Override
    public TokenResponse loginForSwagger(TokenRequest request) {

        UserVO user = findLoginUser(request.username(), request.password());

        return TokenResponse.of(createAccessToken(user), issueRefreshToken(user, true));
    }

    // 이메일·비밀번호로 로그인할 사용자 (틀리면 어느 쪽이 틀렸는지 알려주지 않아요)
    private UserVO findLoginUser(String email, String password) {

        UserVO user = userMapper.selectUserByEmail(email);

        if (user == null || user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
        }

        // 비밀번호까지 맞은 뒤에 알려요(인증 여부로 가입된 이메일인지 알아내지 못하게요).
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new FlowSpaceException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        return user;
    }

    // access token 발급 (토큰에는 번호·이메일·가입 방식만 들어가요)
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

        // 새 이미지 주소는 파일 테이블에 있어서 다시 조회해요
        return UserResponse.from(findUser(email));
    }

    // 프로필 수정
    @Override
    public UserResponse updateProfile(ProfileUpdateRequest request, MultipartFile image, String email) {

        UserVO user = findUser(email);

        if (image != null && !image.isEmpty()) {
            user.setProfileFileId(fileService.uploadImage(image, profileWorkspaceId(user), email).getFileId());
        }

        // 한 줄 소개는 앞뒤 공백을 지우고, 비어 있으면 소개를 지운 것으로 봐요.
        String bio = request.bio() == null ? null : request.bio().trim();

        user.setNickname(request.nickname().trim());
        user.setBio(bio == null || bio.isEmpty() ? null : bio);

        // JPA는 값만 바꾸면 끝날 때 알아서 UPDATE 했지만, MyBatis는 직접 호출해야 DB에 반영돼요
        userMapper.updateUserProfile(user);

        return UserResponse.from(findUser(email));
    }

    // 비밀번호 변경
    // 다른 기기의 로그인은 풀리고(리프레시 토큰을 새로 발급), 지금 쓰는 기기는 새 토큰을 받아 그대로 이어가요.
    @Override
    public LoginResponse changePassword(PasswordChangeRequest request, String email) {

        UserVO user = findUser(email);

        // 구글·마이크로소프트 계정은 비밀번호가 없어요.
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

        // 로그인 상태 유지 여부는 지금 로그인의 남은 기간을 그대로 이어받아요.
        LocalDateTime expiredAt = userMapper.selectRefreshTokenExpiredAt(user.getUserId());

        Duration remaining = expiredAt == null ? SESSION_REFRESH_TTL : Duration.between(LocalDateTime.now(), expiredAt);

        if (remaining.isNegative()) {
            remaining = SESSION_REFRESH_TTL;
        }

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getEmail(), user.getProvider());
        String refreshToken = issueRefreshToken(user, remaining);

        return LoginResponse.from(user, accessToken, refreshToken, user.getLastWorkspaceId());
    }

    // 프로필 이미지를 저장할 워크스페이스: 마지막으로 보던 곳, 없으면 참여 중인 첫 워크스페이스예요(파일은 워크스페이스에 속해야 해요).
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

    // 리프레시 토큰 발급 (MyBatis로 읽은 사용자용): 기존 토큰은 지우고 새로 저장해요(계정당 하나).
    private String issueRefreshToken(UserVO user, Duration ttl) {

        String refreshToken = jwtProvider.createRefreshToken(user.getUserId(), ttl.toMillis());

        userMapper.deleteRefreshToken(user.getUserId());
        userMapper.insertRefreshToken(user.getUserId(), refreshToken, LocalDateTime.now().plus(ttl));

        return refreshToken;
    }

    // 리프레시 토큰 발급: 로그인 상태 유지면 설정된 기간, 아니면 하루 동안 쓸 수 있어요.
    private String issueRefreshToken(UserVO user, boolean remember) {

        Duration ttl = remember ? Duration.ofMillis(jwtProvider.getRefreshTokenExpiration()) : SESSION_REFRESH_TTL;

        return issueRefreshToken(user, ttl);
    }

    // 프로필 이미지 삭제
    @Override
    public UserResponse deleteProfileImage(String email) {

        UserVO user = findUser(email);

        if (user.getProfileFileId() != null) {
            // 사용자 쪽 연결을 먼저 끊고, 파일(서버의 실제 파일과 files 행)을 지워요
            userMapper.updateUserProfileFile(user.getUserId(), null);
            fileService.delete(user.getProfileFileId());
        }

        return UserResponse.from(findUser(email));
    }


    // 소셜 계정으로 로그인할 사용자를 찾아요. 연결된 소셜 계정을 먼저 보고, 없으면 예전 방식(users.provider)으로 가입한 계정을 봐요.
    private UserVO findSocialUser(Provider provider, String providerId) {

        Long userId = userMapper.selectSocialUserId(provider, providerId);

        if (userId == null) {
            userId = userMapper.selectUserIdByProvider(provider, providerId);
        }

        return userId == null ? null : userMapper.selectUserById(userId);
    }

    // 이메일로 가입했지만 인증을 마치지 않은 계정이 있으면, 소셜 계정을 연결하고 인증을 마친 계정으로 바꿔요.
    // 가입 때 정한 비밀번호는 지워요 — 이메일 주인이 아닌 사람이 먼저 가입해 두었을 수 있어서, 주인은 소셜 로그인이나 비밀번호 찾기로 들어와요.
    private UserVO claimUnverifiedAccount(String email, Provider provider, String providerId) {

        UserVO pending = userMapper.selectUserByEmail(email);

        if (pending == null || Boolean.TRUE.equals(pending.getEmailVerified()) || pending.getDeletedAt() != null) {
            return null;
        }

        userMapper.updateUserClaimBySocial(pending.getUserId());
        userMapper.insertSocialAccount(pending.getUserId(), provider, providerId);

        return userMapper.selectUserById(pending.getUserId());
    }

    // 같은 이메일의 계정이 이미 있을 때 새 계정을 만들지 못하게 막아요.
    //  - 비밀번호가 있는 계정(이메일 가입): 그 비밀번호로 본인임을 확인하면 연결할 수 있다고 알려요(SOCIAL_LINK_REQUIRED).
    //    이메일만 보고 합치면 남의 계정을 가로챌 수 있어서, 꼭 비밀번호로 확인해요.
    //  - 그 밖(다른 소셜로 가입한 계정 등): 처음 가입한 방식으로 로그인하게 안내해요(SOCIAL_EMAIL_CONFLICT).
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

    // 소셜 계정 연결: 같은 이메일의 기존 계정(비밀번호가 있는 계정)에 소셜 계정을 연결하고 로그인해요.
    // 소셜 토큰이 그 이메일의 주인임을, 비밀번호가 기존 계정의 주인임을 각각 증명해요.
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

        // 이미 연결된 소셜 계정이면 그대로 로그인해요.
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
            // 소셜 계정이 이 이메일의 주인임을 보여주니, 아직 인증 전이던 계정이면 인증으로 봐요.
            userMapper.updateUserEmailVerified(existing.getUserId());

            user = userMapper.selectUserById(existing.getUserId());
        }

        return socialLoginResponse(user);
    }

    // 소셜 로그인 응답: 토큰 두 개를 발급해요(소셜 로그인은 항상 로그인 상태 유지)
    private LoginResponse socialLoginResponse(UserVO user) {
        return LoginResponse.from(user, createAccessToken(user), issueRefreshToken(user, true),
            user.getLastWorkspaceId());
    }

    // 소셜 회원 생성 공통 처리: 사용자 INSERT → 소셜 계정 연결 → 개인 워크스페이스. 새 워크스페이스 번호를 돌려줘요.
    private Long createSocialUser(UserVO user) {

        // 소셜 계정은 소셜 서비스가 이메일을 확인해 줘서 인증된 것으로 만들어요(비밀번호는 없어요)
        user.setPassword(null);
        user.setEmailVerified(true);

        userMapper.insertUser(user);
        userMapper.insertSocialAccount(user.getUserId(), user.getProvider(), user.getProviderId());

        return workspaceService.createPersonalWorkspace(user.getUserId(), user.getNickname());
    }

    // 소셜 프로필 사진 저장 공통 처리: 파일로 저장하고 사용자 프로필로 지정해요
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

        // 이메일 인증 전인 가입 계정이 있으면 소셜 계정이 이메일 주인임을 보여주니, 그 계정을 넘겨받아요.
        UserVO pending = claimUnverifiedAccount(googleUser.email(), Provider.GOOGLE, googleUser.providerId());

        if (pending != null) {
            return pending;
        }

        // 같은 이메일로 이미 가입한 계정이 있으면 새로 만들지 않아요(연결 확인 또는 안내).
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

        // 프로필 이미지 주소와 마지막으로 본 워크스페이스는 DB에 있어서 다시 조회해요
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

            // Google이 이메일을 확인한 계정만 받아요(확인되지 않은 이메일을 믿으면 남의 이메일로 계정을 만들 수 있어요).
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

        // 이메일 인증 전인 가입 계정이 있으면 소셜 계정이 이메일 주인임을 보여주니, 그 계정을 넘겨받아요.
        UserVO pending = claimUnverifiedAccount(microsoftUser.email(), Provider.MICROSOFT, microsoftUser.providerId());

        if (pending != null) {
            return pending;
        }

        // 같은 이메일로 이미 가입한 계정이 있으면 새로 만들지 않아요(연결 확인 또는 안내).
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
    //  - Google용 검증기로는 Microsoft 토큰을 확인할 수 없어서(발급처·서명 키가 달라요), Microsoft가 공개한 서명 키로 직접 확인해요.
    //  - 서명·만료는 디코더가 확인하고, 이 앱(client-id)용으로 발급됐는지와 Microsoft가 발급했는지는 아래에서 확인해요.
    private MicrosoftUserInfo verifyMicrosoftToken(String idToken) {

        try {

            Jwt jwt = microsoftDecoder().decode(idToken);

            List<String> audience = jwt.getAudience();
            String issuer = jwt.getClaimAsString("iss");

            if (audience == null || !audience.contains(microsoftClientId) || issuer == null
                || !issuer.startsWith(MICROSOFT_ISSUER_PREFIX) || !issuer.endsWith("/v2.0")) {
                throw new FlowSpaceException(ErrorCode.INVALID_LOGIN);
            }

            // 개인 계정은 email 클레임이 없을 수 있어서 preferred_username(로그인 이름)으로 대신해요.
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

    // Microsoft이 공개한 서명 키(JWKS)로 ID Token을 확인하는 디코더 — 처음 쓸 때 한 번만 만들어요(키는 알아서 캐시돼요).
    private JwtDecoder microsoftDecoder() {

        JwtDecoder decoder = microsoftJwtDecoder;

        if (decoder == null) {
            decoder = NimbusJwtDecoder.withJwkSetUri(MICROSOFT_JWK_SET_URI).build();
            microsoftJwtDecoder = decoder;
        }

        return decoder;
    }
}
