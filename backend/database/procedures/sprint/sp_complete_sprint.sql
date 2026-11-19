-- 스프린트 완료 (미완료 작업은 백로그 맨 뒤로 이동, 완료 작업은 유지)
-- 상태 순서 확인과 회고 생성은 Java에서 선처리
--   p_sprint_id  완료할 스프린트 번호

DROP PROCEDURE IF EXISTS sp_complete_sprint;

DELIMITER //

CREATE PROCEDURE sp_complete_sprint(IN p_sprint_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;
    DECLARE v_backlog_count INT;

    SELECT workspace_id INTO v_workspace_id
      FROM sprints
     WHERE sprint_id = p_sprint_id;

    SELECT COUNT(*) INTO v_backlog_count
      FROM tasks
     WHERE workspace_id = v_workspace_id
       AND sprint_id IS NULL;

    UPDATE tasks t
      JOIN (SELECT tk.task_id,
                   ROW_NUMBER() OVER (ORDER BY tk.position, tk.task_id) - 1 AS seq
              FROM tasks tk
              JOIN task_statuses ts ON ts.status_id = tk.status_id
             WHERE tk.sprint_id = p_sprint_id
               AND ts.category <> 'DONE') moved
        ON moved.task_id = t.task_id
       SET t.sprint_id = NULL,
           t.position  = v_backlog_count + moved.seq;

    UPDATE sprints
       SET status = 'COMPLETED'
     WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;
