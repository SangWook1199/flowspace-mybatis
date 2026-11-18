-- 번호로 사용자 한 명을 프로필 이미지 주소와 함께 조회해요. 결과 모양은 sp_user_by_email과 같아요.
--   인자: p_user_id  사용자 번호
--   결과: 0행(없음) 또는 1행. 비밀번호는 암호화된 값이에요(응답에는 넣지 않아요).

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
      LEFT JOIN files f ON f.file_id = u.profile_file_id   -- 프로필 이미지가 없는 사람도 나와야 해요
     WHERE u.user_id = p_user_id;
END //

DELIMITER ;
