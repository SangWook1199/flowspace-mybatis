-- 멤버가 이 워크스페이스에서 맡은 작업 중 진행 중인 수와 완료한 수를 한 번에 세요(프로필 카드용).
--   인자: p_workspace_id  워크스페이스 번호
--         p_user_id       사용자 번호
--   결과: 항상 1행 (맡은 작업이 없으면 0, 0)

DROP PROCEDURE IF EXISTS sp_member_task_count;

DELIMITER //

CREATE PROCEDURE sp_member_task_count(IN p_workspace_id BIGINT, IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COALESCE(SUM(ts.category <> 'DONE'), 0) AS open_task_count,
           COALESCE(SUM(ts.category = 'DONE'), 0)  AS done_task_count
      FROM task_assignees ta
      JOIN tasks t ON t.task_id = ta.task_id
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE ta.user_id = p_user_id
       AND t.workspace_id = p_workspace_id;
END //

DELIMITER ;
