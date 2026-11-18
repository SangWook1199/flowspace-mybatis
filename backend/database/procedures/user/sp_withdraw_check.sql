-- 탈퇴하기 전에, 내가 속한 워크스페이스마다 내 역할과 멤버 수를 조회해요.
-- 자바에서 이 결과로 "탈퇴를 막는 곳 / 함께 지워지는 곳 / 자동으로 나가는 곳"을 나눠요.
--   인자: p_user_id  사용자 번호
--   결과: 워크스페이스 한 개당 한 행. 가입한 순서대로.

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
