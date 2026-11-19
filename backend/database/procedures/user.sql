-- 사용자·로그인 프로시저 (User_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/user.sql"


-- 이메일로 사용자 조회 (프로필 이미지 주소 포함)
--   p_email  이메일
-- 결과: 0행 또는 1행 (password는 암호화된 값)

DROP PROCEDURE IF EXISTS sp_user_by_email;

DELIMITER //

CREATE PROCEDURE sp_user_by_email(IN p_email VARCHAR(100))
    READS SQL DATA
BEGIN
    SELECT u.user_id,
           u.email,
           u.password,
           u.nickname,
           u.provider,
           u.provider_id,
           u.email_verified,
           u.last_active_at,
           u.bio,
           u.deleted_at,
           u.profile_file_id,
           f.file_url AS profile_image_url,
           u.last_workspace_id,
           u.created_at
      FROM users u
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE u.email = p_email;
END //

DELIMITER ;


-- 프로필 수정
--   p_nickname         이름
--   p_bio              소개
--   p_profile_file_id  프로필 이미지 파일 번호
--   p_user_id          사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_user_profile_update;

DELIMITER //

CREATE PROCEDURE sp_user_profile_update(IN p_nickname VARCHAR(30), IN p_bio VARCHAR(100),
                                        IN p_profile_file_id BIGINT, IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE users
       SET nickname        = p_nickname,
           bio             = p_bio,
           profile_file_id = p_profile_file_id
     WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 프로필 이미지 수정
--   p_profile_file_id  프로필 이미지 파일 번호
--   p_user_id          사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_user_profile_file_update;

DELIMITER //

CREATE PROCEDURE sp_user_profile_file_update(IN p_profile_file_id BIGINT, IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE users
       SET profile_file_id = p_profile_file_id
     WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 비밀번호 수정
--   p_password  비밀번호 (암호화된 값)
--   p_user_id   사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_user_password_update;

DELIMITER //

CREATE PROCEDURE sp_user_password_update(IN p_password VARCHAR(255), IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE users
       SET password = p_password
     WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 참여 중인 첫 워크스페이스 번호
--   p_user_id  사용자 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_user_first_workspace_id;

DELIMITER //

CREATE PROCEDURE sp_user_first_workspace_id(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT workspace_id
    FROM workspace_members
    WHERE user_id = p_user_id
    ORDER BY member_id
    LIMIT 1;
END //

DELIMITER ;


-- refresh token 만료 시각
--   p_user_id  사용자 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_refresh_token_expired_at;

DELIMITER //

CREATE PROCEDURE sp_refresh_token_expired_at(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT expired_at
    FROM refresh_tokens
    WHERE user_id = p_user_id;
END //

DELIMITER ;


-- refresh token 삭제
--   p_user_id  사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_refresh_token_delete;

DELIMITER //

CREATE PROCEDURE sp_refresh_token_delete(IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM refresh_tokens
    WHERE user_id = p_user_id;
END //

DELIMITER ;


-- refresh token 저장
--   p_user_id     사용자 번호
--   p_token       토큰
--   p_expired_at  만료 시각
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_refresh_token_insert;

DELIMITER //

CREATE PROCEDURE sp_refresh_token_insert(IN p_user_id BIGINT, IN p_token VARCHAR(500),
                                         IN p_expired_at DATETIME)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO refresh_tokens (user_id, token, expired_at)
    VALUES (p_user_id, p_token, p_expired_at);
END //

DELIMITER ;


-- 번호로 사용자 조회 (프로필 이미지 주소 포함, 컬럼은 sp_user_by_email과 동일)
--   p_user_id  사용자 번호
-- 결과: 0행 또는 1행 (password는 암호화된 값)

DROP PROCEDURE IF EXISTS sp_user;

DELIMITER //

CREATE PROCEDURE sp_user(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT u.user_id,
           u.email,
           u.password,
           u.nickname,
           u.provider,
           u.provider_id,
           u.email_verified,
           u.last_active_at,
           u.bio,
           u.deleted_at,
           u.profile_file_id,
           f.file_url AS profile_image_url,
           u.last_workspace_id,
           u.created_at
      FROM users u
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE u.user_id = p_user_id;
END //

DELIMITER ;


-- 사용자 등록 (생성 시각은 DB 기본값)
--   p_email           이메일
--   p_password        비밀번호 (암호화된 값)
--   p_nickname        이름
--   p_provider        로그인 제공자
--   p_provider_id     제공자 사용자 ID
--   p_email_verified  이메일 인증 여부
--   p_user_id         새 번호
-- 결과: 없음 (p_user_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_user_insert;

DELIMITER //

CREATE PROCEDURE sp_user_insert(IN p_email VARCHAR(100), IN p_password VARCHAR(255), IN p_nickname VARCHAR(30),
                                IN p_provider VARCHAR(50), IN p_provider_id VARCHAR(255),
                                IN p_email_verified BOOLEAN, OUT p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO users (email, password, nickname, provider, provider_id, email_verified)
    VALUES (p_email, p_password, p_nickname, p_provider, p_provider_id, p_email_verified);

    SET p_user_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 인증 전 재가입
--   p_nickname  이름
--   p_password  비밀번호 (암호화된 값)
--   p_user_id   사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_user_signup_restart_update;

DELIMITER //

CREATE PROCEDURE sp_user_signup_restart_update(IN p_nickname VARCHAR(30), IN p_password VARCHAR(255),
                                               IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE users
       SET nickname = p_nickname,
           password = p_password
     WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 이메일 인증 완료
--   p_user_id  사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_user_email_verified_update;

DELIMITER //

CREATE PROCEDURE sp_user_email_verified_update(IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE users
       SET email_verified = TRUE
     WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 인증 전 계정을 소셜 로그인으로 전환
--   p_user_id  사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_user_claim_by_social_update;

DELIMITER //

CREATE PROCEDURE sp_user_claim_by_social_update(IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE users
       SET email_verified = TRUE,
           password       = NULL
     WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 저장된 refresh token
--   p_token  토큰
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_refresh_token;

DELIMITER //

CREATE PROCEDURE sp_refresh_token(IN p_token VARCHAR(500))
    READS SQL DATA
BEGIN
    SELECT user_id, token, expired_at
    FROM refresh_tokens
    WHERE token = p_token;
END //

DELIMITER ;


-- 소셜 계정에 연결된 사용자 번호
--   p_provider     로그인 제공자
--   p_provider_id  제공자 사용자 ID
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_social_account_user_id;

DELIMITER //

CREATE PROCEDURE sp_social_account_user_id(IN p_provider VARCHAR(50), IN p_provider_id VARCHAR(255))
    READS SQL DATA
BEGIN
    SELECT user_id
    FROM user_social_accounts
    WHERE provider = p_provider
      AND provider_id = p_provider_id;
END //

DELIMITER ;


-- 예전 방식으로 가입한 소셜 사용자 번호
--   p_provider     로그인 제공자
--   p_provider_id  제공자 사용자 ID
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_user_id_by_provider;

DELIMITER //

CREATE PROCEDURE sp_user_id_by_provider(IN p_provider VARCHAR(50), IN p_provider_id VARCHAR(255))
    READS SQL DATA
BEGIN
    SELECT user_id
    FROM users
    WHERE provider = p_provider
      AND provider_id = p_provider_id;
END //

DELIMITER ;


-- 소셜 계정 연결 여부
--   p_user_id   사용자 번호
--   p_provider  로그인 제공자
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_social_account_count;

DELIMITER //

CREATE PROCEDURE sp_social_account_count(IN p_user_id BIGINT, IN p_provider VARCHAR(50))
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
    FROM user_social_accounts
    WHERE user_id = p_user_id
      AND provider = p_provider;
END //

DELIMITER ;


-- 소셜 계정 연결
--   p_user_id      사용자 번호
--   p_provider     로그인 제공자
--   p_provider_id  제공자 사용자 ID
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_social_account_insert;

DELIMITER //

CREATE PROCEDURE sp_social_account_insert(IN p_user_id BIGINT, IN p_provider VARCHAR(50),
                                          IN p_provider_id VARCHAR(255))
    MODIFIES SQL DATA
BEGIN
    INSERT INTO user_social_accounts (user_id, provider, provider_id)
    VALUES (p_user_id, p_provider, p_provider_id);
END //

DELIMITER ;


-- 가장 최근에 보낸 메일 시각 (email_verification_tokens)
--   p_user_id  사용자 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_email_token_last_created_at;

DELIMITER //

CREATE PROCEDURE sp_email_token_last_created_at(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT MAX(created_at)
    FROM email_verification_tokens
    WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 사용자의 토큰 전체 삭제
--   p_user_id  사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_email_token_delete;

DELIMITER //

CREATE PROCEDURE sp_email_token_delete(IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM email_verification_tokens
    WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 토큰 저장 (생성 시각은 DB 기본값)
--   p_user_id     사용자 번호
--   p_token_hash  토큰 해시
--   p_expires_at  만료 시각
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_email_token_insert;

DELIMITER //

CREATE PROCEDURE sp_email_token_insert(IN p_user_id BIGINT, IN p_token_hash VARCHAR(64),
                                       IN p_expires_at DATETIME)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO email_verification_tokens (user_id, token_hash, expires_at)
    VALUES (p_user_id, p_token_hash, p_expires_at);
END //

DELIMITER ;


-- 해시로 토큰 찾기
--   p_token_hash  토큰 해시
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_email_token_by_hash;

DELIMITER //

CREATE PROCEDURE sp_email_token_by_hash(IN p_token_hash VARCHAR(64))
    READS SQL DATA
BEGIN
    SELECT user_id, token_hash, expires_at, created_at
    FROM email_verification_tokens
    WHERE token_hash = p_token_hash;
END //

DELIMITER ;


-- 가장 최근에 보낸 메일 시각 (password_reset_tokens)
--   p_user_id  사용자 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_password_token_last_created_at;

DELIMITER //

CREATE PROCEDURE sp_password_token_last_created_at(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT MAX(created_at)
    FROM password_reset_tokens
    WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 사용자의 토큰 전체 삭제
--   p_user_id  사용자 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_password_token_delete;

DELIMITER //

CREATE PROCEDURE sp_password_token_delete(IN p_user_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM password_reset_tokens
    WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 토큰 저장 (생성 시각은 DB 기본값)
--   p_user_id     사용자 번호
--   p_token_hash  토큰 해시
--   p_expires_at  만료 시각
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_password_token_insert;

DELIMITER //

CREATE PROCEDURE sp_password_token_insert(IN p_user_id BIGINT, IN p_token_hash VARCHAR(64),
                                          IN p_expires_at DATETIME)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
    VALUES (p_user_id, p_token_hash, p_expires_at);
END //

DELIMITER ;


-- 해시로 토큰 찾기
--   p_token_hash  토큰 해시
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_password_token_by_hash;

DELIMITER //

CREATE PROCEDURE sp_password_token_by_hash(IN p_token_hash VARCHAR(64))
    READS SQL DATA
BEGIN
    SELECT user_id, token_hash, expires_at, created_at
    FROM password_reset_tokens
    WHERE token_hash = p_token_hash;
END //

DELIMITER ;


-- 재설정 토큰 삭제
--   p_token_hash  토큰 해시
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_password_token_hash_delete;

DELIMITER //

CREATE PROCEDURE sp_password_token_hash_delete(IN p_token_hash VARCHAR(64))
    MODIFIES SQL DATA
BEGIN
    DELETE FROM password_reset_tokens
    WHERE token_hash = p_token_hash;
END //

DELIMITER ;


-- 탈퇴 전 확인용 소속 워크스페이스 (내 역할, 멤버 수)
--   p_user_id  사용자 번호
-- 결과: 워크스페이스당 한 행, 참여 순

DROP PROCEDURE IF EXISTS sp_withdraw_check;

DELIMITER //

CREATE PROCEDURE sp_withdraw_check(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT w.workspace_id,
           w.name,
           w.owner_id,
           wm.role,
           (SELECT COUNT(*)
              FROM workspace_members m
             WHERE m.workspace_id = w.workspace_id) AS member_count
      FROM workspace_members wm
      JOIN workspaces w ON w.workspace_id = wm.workspace_id
     WHERE wm.user_id = p_user_id
     ORDER BY wm.member_id;
END //

DELIMITER ;


-- 회원 탈퇴 (사용자 행은 유지하고 개인정보만 삭제, 작업·댓글·활동 기록 보존)
--   1) 다른 사용자의 워크스페이스에서 나가기  2) 단독 소유 워크스페이스 삭제
--   3) 연결된 기록 삭제 (작업 담당, 알림, 토큰, 소셜 계정, 대기 중인 초대)  4) 개인정보 익명 처리 (이메일은 재가입 가능한 값으로 변경)
-- 본인 확인과 다른 멤버가 있는 소유 워크스페이스 확인은 Java에서 선처리
--   p_user_id   탈퇴할 사용자 번호
--   p_nickname  탈퇴 후 표시 이름 (AccountServiceImpl.WITHDRAWN_NICKNAME)

DROP PROCEDURE IF EXISTS sp_withdraw_user;

DELIMITER //

CREATE PROCEDURE sp_withdraw_user(IN p_user_id BIGINT, IN p_nickname VARCHAR(30))
    MODIFIES SQL DATA
BEGIN
    DECLARE v_email VARCHAR(100);

    SELECT email INTO v_email
      FROM users
     WHERE user_id = p_user_id;

    DELETE FROM workspace_members
     WHERE user_id = p_user_id
       AND role <> 'OWNER';

    DELETE FROM tasks
     WHERE workspace_id IN (SELECT workspace_id FROM workspaces WHERE owner_id = p_user_id);

    DELETE FROM workspaces
     WHERE owner_id = p_user_id;

    DELETE FROM task_assignees        WHERE user_id = p_user_id;
    DELETE FROM notifications         WHERE user_id = p_user_id;
    DELETE FROM refresh_tokens        WHERE user_id = p_user_id;
    DELETE FROM user_social_accounts  WHERE user_id = p_user_id;
    DELETE FROM password_reset_tokens WHERE user_id = p_user_id;

    DELETE FROM workspace_invites
     WHERE email = v_email
       AND status = 'PENDING';

    UPDATE users
       SET email             = CONCAT('withdrawn-', p_user_id, '@deleted.flowspace'),
           nickname          = p_nickname,
           password          = NULL,
           provider_id       = NULL,
           bio               = NULL,
           profile_file_id   = NULL,
           last_workspace_id = NULL,
           deleted_at        = NOW()
     WHERE user_id = p_user_id;
END //

DELIMITER ;


-- 이메일이 정확히 일치하는 사용자 조회 + 워크스페이스 멤버·초대 여부 (초대 화면용)
--   p_email         이메일 (정확히 일치할 때만, 부분 일치 검색 방지)
--   p_workspace_id  워크스페이스 번호 (NULL이면 멤버·초대 여부는 0)
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_user_lookup;

DELIMITER //

CREATE PROCEDURE sp_user_lookup(IN p_email VARCHAR(100), IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT u.user_id,
           u.nickname,
           u.email,
           f.file_url AS profile_image_url,
           EXISTS (SELECT 1
                     FROM workspace_members wm
                    WHERE wm.workspace_id = p_workspace_id
                      AND wm.user_id = u.user_id) AS is_member,
           EXISTS (SELECT 1
                     FROM workspace_invites wi
                    WHERE wi.workspace_id = p_workspace_id
                      AND wi.email = u.email
                      AND wi.status = 'PENDING') AS is_invited
      FROM users u
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE u.email = p_email;
END //

DELIMITER ;


-- ---------- 다른 Mapper 공용 ----------

-- 이메일로 사용자 번호
--   p_email  이메일
-- 결과: 0행 또는 1행 (user_id)

DROP PROCEDURE IF EXISTS sp_user_id_by_email;

DELIMITER //

CREATE PROCEDURE sp_user_id_by_email(IN p_email VARCHAR(100))
    READS SQL DATA
BEGIN
    SELECT user_id
      FROM users
     WHERE email = p_email;
END //

DELIMITER ;
