-- 작업 상태(칸반 열) 삭제 + 해당 상태 작업을 다른 상태로 이동
-- 기본 상태(공용 행)는 연결만 해제, 워크스페이스 전용 상태는 삭제
-- 마지막 상태 여부와 대상 상태 확인은 Java에서 선처리, 완료 시각은 syncCompletedAt과 같은 규칙
--   p_workspace_id      워크스페이스 번호
--   p_status_id         삭제할 상태 번호
--   p_target_status_id  작업을 옮길 상태 번호

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

    UPDATE tasks
       SET status_id    = p_target_status_id,
           completed_at = CASE WHEN v_target_category = 'DONE' THEN COALESCE(completed_at, NOW()) ELSE NULL END
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;

    DELETE FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_status_id;

    IF NOT v_is_default THEN
        DELETE FROM task_statuses
         WHERE status_id = p_status_id;
    END IF;
END //

DELIMITER ;
