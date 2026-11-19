-- 캘린더·일정 프로시저 (Calendar_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/calendar.sql"


-- 일정 등록 (생성·수정 시각은 DB 기본값)
--   p_workspace_id    워크스페이스 번호
--   p_created_by      만든 사용자 번호
--   p_title           제목
--   p_description     설명
--   p_color           색상
--   p_start_datetime  시작 시각
--   p_end_datetime    종료 시각 (NULL이면 없음)
--   p_event_id        새 번호
-- 결과: 없음 (p_event_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_event_insert;

DELIMITER //

CREATE PROCEDURE sp_event_insert(IN p_workspace_id BIGINT, IN p_created_by BIGINT, IN p_title VARCHAR(200),
                                 IN p_description TEXT, IN p_color VARCHAR(50), IN p_start_datetime DATETIME,
                                 IN p_end_datetime DATETIME, OUT p_event_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO events (workspace_id, created_by, title, description, color, start_datetime, end_datetime)
    VALUES (p_workspace_id, p_created_by, p_title, p_description, p_color, p_start_datetime, p_end_datetime);

    SET p_event_id = LAST_INSERT_ID();
END //

DELIMITER ;


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


-- 일정 수정 (updated_at은 ON UPDATE CURRENT_TIMESTAMP)
--   p_title           제목
--   p_description     설명
--   p_color           색상
--   p_start_datetime  시작 시각
--   p_end_datetime    종료 시각 (NULL이면 없음)
--   p_event_id        일정 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_event_update;

DELIMITER //

CREATE PROCEDURE sp_event_update(IN p_title VARCHAR(200), IN p_description TEXT, IN p_color VARCHAR(50),
                                 IN p_start_datetime DATETIME, IN p_end_datetime DATETIME,
                                 IN p_event_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE events
    SET title          = p_title,
        description    = p_description,
        color          = p_color,
        start_datetime = p_start_datetime,
        end_datetime   = p_end_datetime
    WHERE event_id = p_event_id;
END //

DELIMITER ;


-- 일정 삭제 (블록의 event_id는 ON DELETE SET NULL)
--   p_event_id  일정 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_event_delete;

DELIMITER //

CREATE PROCEDURE sp_event_delete(IN p_event_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM events
    WHERE event_id = p_event_id;
END //

DELIMITER ;


-- 제목으로 일정 검색 (대소문자 구분 없음)
--   p_workspace_id  워크스페이스 번호
--   p_keyword       검색어
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_event_search_list;

DELIMITER //

CREATE PROCEDURE sp_event_search_list(IN p_workspace_id BIGINT, IN p_keyword VARCHAR(255))
    READS SQL DATA
BEGIN
    SELECT event_id,
           title,
           start_datetime,
           end_datetime
    FROM events
    WHERE workspace_id = p_workspace_id
      AND LOWER(title) LIKE CONCAT('%', LOWER(p_keyword), '%')
    ORDER BY event_id;
END //

DELIMITER ;


-- 스프린트가 속한 워크스페이스
--   p_sprint_id  스프린트 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_sprint_workspace_id;

DELIMITER //

CREATE PROCEDURE sp_sprint_workspace_id(IN p_sprint_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT workspace_id
    FROM sprints
    WHERE sprint_id = p_sprint_id;
END //

DELIMITER ;


-- 기본 스프린트 (진행 중 → 계획 중, 시작일 순)
--   p_workspace_id  워크스페이스 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_default_sprint_id;

DELIMITER //

CREATE PROCEDURE sp_default_sprint_id(IN p_workspace_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT sprint_id
    FROM sprints
    WHERE workspace_id = p_workspace_id
      AND status IN ('ACTIVE', 'PLANNING')
    ORDER BY status = 'ACTIVE' DESC, start_date, sprint_id
    LIMIT 1;
END //

DELIMITER ;


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


-- 그 달에 시작하는 일정
--   p_workspace_id    워크스페이스 번호
--   p_start_datetime  그 달 첫날 0시
--   p_end_datetime    그 달 마지막 날 끝
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_calendar_event_list;

DELIMITER //

CREATE PROCEDURE sp_calendar_event_list(IN p_workspace_id BIGINT, IN p_start_datetime DATETIME,
                                        IN p_end_datetime DATETIME)
    READS SQL DATA
BEGIN
    SELECT event_id,
           workspace_id,
           title,
           description,
           color,
           start_datetime,
           end_datetime
    FROM events
    WHERE workspace_id = p_workspace_id
      AND start_datetime BETWEEN p_start_datetime AND p_end_datetime
    ORDER BY start_datetime, event_id;
END //

DELIMITER ;
