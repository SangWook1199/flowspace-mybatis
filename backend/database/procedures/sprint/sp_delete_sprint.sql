-- 스프린트 삭제 (작업은 백로그 맨 뒤로 이동)
-- 권한 확인(관리자 이상)은 Java에서 선처리
--   p_sprint_id  삭제할 스프린트 번호

DROP PROCEDURE IF EXISTS sp_delete_sprint;

DELIMITER //

CREATE PROCEDURE sp_delete_sprint(IN p_sprint_id BIGINT)
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
