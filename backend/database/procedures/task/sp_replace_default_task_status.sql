-- 기본 상태(모든 워크스페이스가 같이 쓰는 행)를 수정할 때, 이 워크스페이스만 새 상태로 바꿔 끼워요.
-- 공용 행을 고치면 다른 워크스페이스도 바뀌기 때문이에요.
--   인자: p_workspace_id     워크스페이스 번호
--         p_old_status_id    수정하려던 기본 상태 번호
--         p_new_status_id    자바에서 먼저 만든 새 상태 번호 (이름·분류·색이 들어 있어요)
--         p_wip_limit        WIP 제한 (NULL이면 제한 없음)
--   완료 시각은 자바의 syncCompletedAt과 같은 규칙이에요(컨테이너 시간대가 Asia/Seoul이라 NOW()가 한국 시각이에요).

DROP PROCEDURE IF EXISTS sp_replace_default_task_status;

DELIMITER //

CREATE PROCEDURE sp_replace_default_task_status(IN p_workspace_id BIGINT, IN p_old_status_id BIGINT,
                                                IN p_new_status_id BIGINT, IN p_wip_limit INT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_position INT;
    DECLARE v_category VARCHAR(20);

    -- 기존 상태가 있던 칸반 열 위치와, 새 상태의 분류
    SELECT position INTO v_position
      FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_old_status_id;

    SELECT category INTO v_category
      FROM task_statuses
     WHERE status_id = p_new_status_id;

    -- 1. 같은 자리에 새 상태를 연결해요
    INSERT INTO workspace_task_statuses (workspace_id, status_id, position, is_default, wip_limit)
    VALUES (p_workspace_id, p_new_status_id, v_position, FALSE, p_wip_limit);

    -- 2. 이 워크스페이스의 작업만 새 상태로 옮겨요
    UPDATE tasks
       SET status_id    = p_new_status_id,
           completed_at = CASE WHEN v_category = 'DONE' THEN COALESCE(completed_at, NOW()) ELSE NULL END
     WHERE workspace_id = p_workspace_id
       AND status_id = p_old_status_id;

    -- 3. 이 워크스페이스에서 기존 기본 상태 연결을 끊어요 (공용 행은 그대로)
    DELETE FROM workspace_task_statuses
     WHERE workspace_id = p_workspace_id
       AND status_id = p_old_status_id;
END //

DELIMITER ;
