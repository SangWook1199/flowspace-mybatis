-- 멤버의 미완료 담당 작업 (프로필 카드용)
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호
--   p_limit         최대 개수
-- 결과: 마감일 순 (마감일 없음은 맨 뒤, 그 안에서는 최신 생성 순)

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
     ORDER BY t.end_date IS NULL,
              t.end_date,
              t.task_id DESC
     LIMIT p_limit;
END //

DELIMITER ;
