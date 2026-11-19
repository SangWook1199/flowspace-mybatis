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
