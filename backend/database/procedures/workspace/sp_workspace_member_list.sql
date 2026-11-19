-- 워크스페이스 멤버 목록 (이름·프로필 이미지·역할 포함)
--   p_workspace_id  워크스페이스 번호
-- 결과: 멤버당 한 행, 참여 순

DROP PROCEDURE IF EXISTS sp_workspace_member_list;

DELIMITER //

CREATE PROCEDURE sp_workspace_member_list(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT wm.workspace_id,
           u.user_id,
           u.nickname,
           u.email,
           u.bio,
           u.profile_file_id,
           f.file_url AS profile_image_url,
           u.last_active_at,
           wm.role,
           wm.joined_at
      FROM workspace_members wm
      JOIN users u ON u.user_id = wm.user_id
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE wm.workspace_id = p_workspace_id
     ORDER BY wm.member_id;
END //

DELIMITER ;
