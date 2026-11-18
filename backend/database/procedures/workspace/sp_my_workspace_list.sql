-- 내가 속한 워크스페이스 목록과 각 워크스페이스에서 내 역할을 조회해요.
--   인자: p_user_id  사용자 번호
--   결과: 워크스페이스 한 개당 한 행. 가입한 순서대로.

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
