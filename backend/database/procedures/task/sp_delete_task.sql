-- 작업 삭제 + 같은 칸(스프린트·상태)의 남은 작업 순서 재정렬 (0부터)
-- 담당자·하위 작업·댓글·첨부는 ON DELETE CASCADE
-- 멤버 확인과 첨부 파일(서버 파일) 삭제는 Java에서 처리
--   p_task_id  삭제할 작업 번호

DROP PROCEDURE IF EXISTS sp_delete_task;

DELIMITER //

CREATE PROCEDURE sp_delete_task(IN p_task_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;
    DECLARE v_sprint_id BIGINT;
    DECLARE v_status_id BIGINT;

    SELECT workspace_id, sprint_id, status_id
      INTO v_workspace_id, v_sprint_id, v_status_id
      FROM tasks
     WHERE task_id = p_task_id;

    DELETE FROM tasks
     WHERE task_id = p_task_id;

    UPDATE tasks t
      JOIN (SELECT task_id,
                   ROW_NUMBER() OVER (ORDER BY position, task_id) - 1 AS seq
              FROM tasks
             WHERE workspace_id = v_workspace_id
               AND sprint_id <=> v_sprint_id
               AND status_id = v_status_id) remain
        ON remain.task_id = t.task_id
       SET t.position = remain.seq;
END //

DELIMITER ;
