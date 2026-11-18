-- 스프린트를 완료해요. 끝나지 않은 작업만 백로그 맨 뒤로 옮기고, 완료된 작업은 스프린트에 남겨요.
-- 작업마다 UPDATE를 보내던 것을 UPDATE 두 번으로 합쳤어요.
--   인자: p_sprint_id  완료할 스프린트 번호
--   상태 순서 확인과 회고 생성은 자바 서비스에서 먼저 해요.

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

    -- 지금 백로그에 있는 작업 수 (옮겨 오는 작업은 이 번호부터 순서를 받아요)
    SELECT COUNT(*) INTO v_backlog_count
      FROM tasks
     WHERE workspace_id = v_workspace_id
       AND sprint_id IS NULL;

    -- 끝나지 않은(DONE이 아닌) 작업만 원래 순서대로 백로그 뒤에 붙여요
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
