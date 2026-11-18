-- 스프린트를 삭제해요. 속한 작업은 지우지 않고 백로그 맨 뒤로 옮겨요.
-- 작업마다 UPDATE를 보내던 것을 UPDATE 한 번과 DELETE 한 번으로 합쳤어요.
--   인자: p_sprint_id  삭제할 스프린트 번호
--   권한 확인(관리자 이상)은 자바 서비스에서 먼저 해요.

DROP PROCEDURE IF EXISTS sp_delete_sprint;

DELIMITER //

CREATE PROCEDURE sp_delete_sprint(IN p_sprint_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;
    DECLARE v_backlog_count INT;

    -- 스프린트가 속한 워크스페이스
    SELECT workspace_id INTO v_workspace_id
      FROM sprints
     WHERE sprint_id = p_sprint_id;

    -- 지금 백로그에 있는 작업 수 (옮겨 오는 작업은 이 번호부터 순서를 받아요)
    SELECT COUNT(*) INTO v_backlog_count
      FROM tasks
     WHERE workspace_id = v_workspace_id
       AND sprint_id IS NULL;

    -- 스프린트의 작업을 원래 순서대로 백로그 뒤에 붙여요
    UPDATE tasks t
      JOIN (SELECT task_id,
                   ROW_NUMBER() OVER (ORDER BY position, task_id) - 1 AS seq
              FROM tasks
             WHERE sprint_id = p_sprint_id) moved
        ON moved.task_id = t.task_id
       SET t.sprint_id = NULL,
           t.position  = v_backlog_count + moved.seq;

    DELETE FROM sprints
     WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;
