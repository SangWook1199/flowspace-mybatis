-- 일정 한 건 (작성자 이름 포함)
--   p_event_id  일정 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_event;

DELIMITER //

CREATE PROCEDURE sp_event(IN p_event_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT e.event_id,
           e.workspace_id,
           e.created_by,
           u.nickname AS created_by_name,
           e.title,
           e.description,
           e.color,
           e.start_datetime,
           e.end_datetime,
           e.created_at
      FROM events e
      JOIN users u ON u.user_id = e.created_by
     WHERE e.event_id = p_event_id;
END //

DELIMITER ;
