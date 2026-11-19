-- 캘린더 월별 작업 목록 (시작일이 해당 월인 작업, 상태 색 포함)
--   p_workspace_id  워크스페이스 번호
--   p_sprint_id     스프린트 번호 (NULL이면 백로그)
--   p_start_date    월 시작일
--   p_end_date      월 종료일
-- 결과: 작업당 한 행, 시작일 → position 순

DROP PROCEDURE IF EXISTS sp_calendar_task_list;

DELIMITER //

CREATE PROCEDURE sp_calendar_task_list(IN p_workspace_id BIGINT, IN p_sprint_id BIGINT,
                                       IN p_start_date DATE, IN p_end_date DATE)
    READS SQL DATA
BEGIN
    SELECT t.task_id,
           t.sprint_id,
           t.status_id,
           ts.color AS status_color,
           t.title,
           t.description,
           t.start_date,
           t.end_date
      FROM tasks t
      JOIN task_statuses ts ON ts.status_id = t.status_id
     WHERE t.workspace_id = p_workspace_id
       AND t.sprint_id <=> p_sprint_id
       AND t.start_date BETWEEN p_start_date AND p_end_date
     ORDER BY t.start_date, t.position, t.task_id;
END //

DELIMITER ;
