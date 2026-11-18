-- 새 워크스페이스에 소유자와 기본 작업 상태 3개(할 일·진행 중·완료)를 연결해요.
-- 워크스페이스 행은 자바에서 먼저 INSERT 해서 번호를 받아 와요.
--   인자: p_workspace_id  새 워크스페이스 번호
--         p_owner_id      만든 사람 번호
--   기본 상태 1, 2, 3번은 모든 워크스페이스가 같이 쓰는 행이에요(ddl.sql에서 만들어요).

DROP PROCEDURE IF EXISTS sp_init_workspace;

DELIMITER //

CREATE PROCEDURE sp_init_workspace(IN p_workspace_id BIGINT, IN p_owner_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    -- 1. 만든 사람을 소유자로
    INSERT INTO workspace_members (workspace_id, user_id, role)
    VALUES (p_workspace_id, p_owner_id, 'OWNER');

    -- 2. 기본 상태를 칸반 열 순서대로 연결
    INSERT INTO workspace_task_statuses (workspace_id, status_id, position, is_default)
    VALUES (p_workspace_id, 1, 0, TRUE),
           (p_workspace_id, 2, 1, TRUE),
           (p_workspace_id, 3, 2, TRUE);
END //

DELIMITER ;
