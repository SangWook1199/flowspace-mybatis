-- 작업 상태(칸반 열)를 삭제해요. 그 상태의 작업은 지정한 다른 상태로 옮겨요.
--   인자: p_workspace_id      워크스페이스 번호
--         p_status_id         삭제할 상태 번호
--         p_target_status_id  작업을 옮길 상태 번호
--   마지막 상태인지, 옮길 상태가 같은 워크스페이스인지는 자바 서비스에서 먼저 확인해요.
--   완료 시각은 자바의 syncCompletedAt과 같은 규칙이에요.

DROP PROCEDURE IF EXISTS sp_delete_task_status;

DELIMITER //

CREATE PROCEDURE sp_delete_task_status(IN p_workspace_id BIGINT, IN p_status_id BIGINT,
                                       IN p_target_status_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_is_default BOOLEAN;
    DECLARE v_target_category VARCHAR(20);

    SELECT is_default INTO v_is_default
      FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;

    SELECT category INTO v_target_category
      FROM task_statuses
     WHERE status_id = p_target_status_id;

    -- 1. 이 워크스페이스의 작업을 옮길 상태로 보내요
    UPDATE tasks
       SET status_id    = p_target_status_id,
           completed_at = CASE WHEN v_target_category = 'DONE' THEN COALESCE(completed_at, NOW()) ELSE NULL END
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;

    -- 2. 이 워크스페이스와 상태의 연결을 끊어요
    DELETE FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;

    -- 3. 기본 상태는 모든 워크스페이스가 같이 쓰는 행이라 남기고, 이 워크스페이스 전용 상태만 지워요
    IF NOT v_is_default THEN
        DELETE FROM task_statuses
         WHERE status_id = p_status_id;
    END IF;
END //

DELIMITER ;
