-- 내 워크스페이스 목록 + 역할
--   p_user_id  사용자 번호
-- 결과: 워크스페이스당 한 행, 참여 순

DROP PROCEDURE IF EXISTS sp_my_workspace_list;

DELIMITER //

CREATE PROCEDURE sp_my_workspace_list(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT w.workspace_id,
           w.owner_id,
           w.name,
           w.initials,
           w.color,
           w.icon,
           w.created_at,
           wm.role
      FROM workspace_members wm
      JOIN workspaces w ON w.workspace_id = wm.workspace_id
     WHERE wm.user_id = p_user_id
     ORDER BY wm.member_id;
END //

DELIMITER ;
