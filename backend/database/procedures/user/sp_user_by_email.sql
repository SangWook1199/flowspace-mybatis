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
