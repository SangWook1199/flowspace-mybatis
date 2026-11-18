-- 작업을 삭제하고, 같은 칸(같은 스프린트·같은 상태)에 남은 작업의 순서를 0부터 다시 매겨요.
-- 작업마다 UPDATE를 보내던 것을 DELETE 한 번과 UPDATE 한 번으로 합쳤어요.
--   인자: p_task_id  삭제할 작업 번호
--   담당자·하위 작업·댓글·첨부 행은 외래 키(ON DELETE CASCADE)로 DB가 함께 지워요.
--   멤버 확인과 첨부 파일(서버에 올린 실제 파일) 삭제는 자바 서비스에서 해요.

DROP PROCEDURE IF EXISTS sp_delete_task;

DELIMITER //

CREATE PROCEDURE sp_delete_task(IN p_task_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DECLARE v_workspace_id BIGINT;
    DECLARE v_sprint_id BIGINT;
    DECLARE v_status_id BIGINT;

    -- 지우기 전에 어느 칸에 있던 작업인지 기억해요
    SELECT workspace_id, sprint_id, status_id
      INTO v_workspace_id, v_sprint_id, v_status_id
      FROM tasks
     WHERE task_id = p_task_id;

    DELETE FROM tasks
     WHERE task_id = p_task_id;

    -- 같은 칸에 남은 작업에 원래 순서대로 0, 1, 2 … 를 다시 매겨요 (중간에 빈 번호가 생기지 않게)
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
