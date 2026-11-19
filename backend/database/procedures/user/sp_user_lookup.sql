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
