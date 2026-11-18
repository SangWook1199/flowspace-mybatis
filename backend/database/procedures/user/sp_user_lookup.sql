-- 이메일이 정확히 같은 사용자 한 명을, 그 워크스페이스에 이미 멤버인지·이미 초대했는지와 함께 조회해요
-- (워크스페이스에 초대할 때 "이 사람이 맞나요?"를 보여주는 용도).
--   인자: p_email         찾을 이메일 (정확히 같을 때만 찾아요 — 부분 일치로 사용자를 훑어볼 수 없게)
--         p_workspace_id  워크스페이스 번호 (NULL이면 멤버·초대 여부는 항상 0)
--   결과: 0행(없음) 또는 1행

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
