-- 멤버가 이 워크스페이스에서 맡은, 아직 끝나지 않은 작업을 조회해요(프로필 카드용).
--   인자: p_workspace_id  워크스페이스 번호
--         p_user_id       사용자 번호
--         p_limit         최대 개수
--   결과: 마감일이 빠른 순. 마감일이 없는 작업은 맨 뒤, 그 안에서는 최근에 만든 작업부터.

DROP PROCEDURE IF EXISTS sp_member_open_task_list;

DELIMITER //

CREATE PROCEDURE sp_member_open_task_list(IN p_workspace_id BIGINT, IN p_user_id BIGINT, IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT t.task_id,
           t.sprint_id,
           t.title,
           t.end_date,
           t.priority,
           ts.name AS status_name
      FROM task_assignees ta
      JOIN tasks t ON t.task_id = ta.task_id
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE ta.user_id = p_user_id
       AND t.workspace_id = p_workspace_id
       AND ts.category <> 'DONE'
     ORDER BY t.end_date IS NULL,   -- 마감일이 없으면 1이라 뒤로 가요
              t.end_date,
              t.task_id DESC
     LIMIT p_limit;
END //

DELIMITER ;
