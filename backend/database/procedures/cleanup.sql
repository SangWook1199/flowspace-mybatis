-- 만료 데이터 정리 프로시저 (Cleanup_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/cleanup.sql"


-- 만료된 리프레시 토큰 삭제
--   p_now  기준 시각 (이보다 먼저 만료된 것 삭제)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_refresh_token_expired_delete;

DELIMITER //

CREATE PROCEDURE sp_refresh_token_expired_delete(IN p_now DATETIME)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM refresh_tokens
    WHERE expired_at < p_now;
END //

DELIMITER ;


-- 만료된 비밀번호 재설정 토큰 삭제
--   p_now  기준 시각 (이보다 먼저 만료된 것 삭제)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_password_token_expired_delete;

DELIMITER //

CREATE PROCEDURE sp_password_token_expired_delete(IN p_now DATETIME)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM password_reset_tokens
    WHERE expires_at < p_now;
END //

DELIMITER ;


-- 만료된 이메일 인증 토큰 삭제
--   p_now  기준 시각 (이보다 먼저 만료된 것 삭제)
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_email_token_expired_delete;

DELIMITER //

CREATE PROCEDURE sp_email_token_expired_delete(IN p_now DATETIME)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM email_verification_tokens
    WHERE expires_at < p_now;
END //

DELIMITER ;


-- 오래된 대기(PENDING) 초대 삭제
--   p_before  이 시각 이전에 보낸 초대 삭제
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_invite_stale_delete;

DELIMITER //

CREATE PROCEDURE sp_invite_stale_delete(IN p_before DATETIME)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM workspace_invites
    WHERE status = 'PENDING'
      AND created_at < p_before;
END //

DELIMITER ;
