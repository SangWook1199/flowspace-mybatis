-- 기본 상태(공용 행) 수정 시 해당 워크스페이스만 새 상태로 교체 (공용 행은 유지)
-- 완료 시각은 syncCompletedAt과 같은 규칙 (NOW()는 Asia/Seoul 기준)
--   p_workspace_id   워크스페이스 번호
--   p_old_status_id  기존 기본 상태 번호
--   p_new_status_id  Java에서 생성한 새 상태 번호
--   p_wip_limit      WIP 제한 (NULL이면 제한 없음)

DROP PROCEDURE IF EXISTS sp_replace_default_task_status;

DELIMITER //

CREATE PROCEDURE sp_replace_default_task_status(IN p_workspace_id BIGINT, IN p_old_status_id BIGINT,
                                                IN p_new_status_id BIGINT, IN p_wip_limit INT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_position INT;
    DECLARE v_category VARCHAR(20);

    SELECT position INTO v_position
      FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_old_status_id;

    SELECT category INTO v_category
      FROM task_statuses
     WHERE status_id = p_new_status_id;

    INSERT INTO workspace_task_statuses (workspace_id, status_id, position, is_default, wip_limit)
    VALUES (p_workspace_id, p_new_status_id, v_position, FALSE, p_wip_limit);

    UPDATE tasks
       SET status_id    = p_new_status_id,
           completed_at = CASE WHEN v_category = 'DONE' THEN COALESCE(completed_at, NOW()) ELSE NULL END
     WHERE workspace_id = p_workspace_id
       AND status_id = p_old_status_id;

    DELETE FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_old_status_id;
END //

DELIMITER ;
