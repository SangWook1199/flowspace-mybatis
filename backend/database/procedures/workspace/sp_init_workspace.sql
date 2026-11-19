-- 새 워크스페이스에 소유자와 기본 작업 상태 3개(할 일·진행 중·완료) 연결
-- 워크스페이스 행은 Java에서 먼저 등록, 기본 상태 1~3번은 공용 행 (ddl.sql)
--   p_workspace_id  워크스페이스 번호
--   p_owner_id      생성자 번호

DROP PROCEDURE IF EXISTS sp_init_workspace;

DELIMITER //

CREATE PROCEDURE sp_init_workspace(IN p_workspace_id BIGINT, IN p_owner_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO workspace_members (workspace_id, user_id, role)
    VALUES (p_workspace_id, p_owner_id, 'OWNER');

    INSERT INTO workspace_task_statuses (workspace_id, status_id, position, is_default)
    VALUES (p_workspace_id, 1, 0, TRUE),
           (p_workspace_id, 2, 1, TRUE),
           (p_workspace_id, 3, 2, TRUE);
END //

DELIMITER ;
