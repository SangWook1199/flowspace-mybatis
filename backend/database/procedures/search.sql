-- 통합 검색 프로시저 (Search_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/search.sql"


-- 제목으로 페이지 검색 (휴지통 제외)
--   p_workspace_id  워크스페이스 번호
--   p_like          LIKE 패턴 (소문자, '!'로 특수문자 이스케이프)
--   p_limit         최대 행 수
-- 결과: page_id 역순 (id·title·icon)

DROP PROCEDURE IF EXISTS sp_search_page_list;

DELIMITER //

CREATE PROCEDURE sp_search_page_list(IN p_workspace_id BIGINT, IN p_like VARCHAR(255), IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT page_id AS id,
           title,
           icon
      FROM pages
     WHERE workspace_id = p_workspace_id
       AND is_deleted = FALSE
       AND LOWER(title) LIKE p_like ESCAPE '!'
     ORDER BY page_id DESC
     LIMIT p_limit;
END //

DELIMITER ;


-- 본문 검색 후보 블록 (휴지통 페이지 제외, 블록 내용 JSON을 글자로 바꿔 LIKE)
--   p_workspace_id  워크스페이스 번호
--   p_like          LIKE 패턴 (소문자, '!'로 특수문자 이스케이프)
-- 결과: 최대 100행, block_id 역순 (id·page_id·body·페이지 title·icon)

DROP PROCEDURE IF EXISTS sp_search_block_list;

DELIMITER //

CREATE PROCEDURE sp_search_block_list(IN p_workspace_id BIGINT, IN p_like VARCHAR(255))
    READS SQL DATA
BEGIN
    SELECT b.block_id AS id,
           b.page_id,
           b.content  AS body,
           p.title,
           p.icon
      FROM blocks b
      JOIN pages p ON p.page_id = b.page_id
     WHERE p.workspace_id = p_workspace_id
       AND p.is_deleted = FALSE
       AND LOWER(CAST(b.content AS CHAR)) LIKE p_like ESCAPE '!'
     ORDER BY b.block_id DESC
     LIMIT 100;
END //

DELIMITER ;


-- 작업 검색 (제목·설명, 상태 이름 포함)
--   p_workspace_id  워크스페이스 번호
--   p_like          LIKE 패턴 (소문자, '!'로 특수문자 이스케이프)
--   p_limit         최대 행 수
-- 결과: task_id 역순 (id·title·body·meta(상태 이름)·sprint_id)

DROP PROCEDURE IF EXISTS sp_search_task_list;

DELIMITER //

CREATE PROCEDURE sp_search_task_list(IN p_workspace_id BIGINT, IN p_like VARCHAR(255), IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT t.task_id     AS id,
           t.title,
           t.description AS body,
           s.name        AS meta,
           t.sprint_id
      FROM tasks t
      JOIN task_statuses s ON s.status_id = t.status_id
     WHERE t.workspace_id = p_workspace_id
       AND (LOWER(t.title) LIKE p_like ESCAPE '!'
            OR LOWER(COALESCE(t.description, '')) LIKE p_like ESCAPE '!')
     ORDER BY t.task_id DESC
     LIMIT p_limit;
END //

DELIMITER ;


-- 스프린트 검색 (이름·목표)
--   p_workspace_id  워크스페이스 번호
--   p_like          LIKE 패턴 (소문자, '!'로 특수문자 이스케이프)
--   p_limit         최대 행 수
-- 결과: sprint_id 역순 (id·title·body)

DROP PROCEDURE IF EXISTS sp_search_sprint_list;

DELIMITER //

CREATE PROCEDURE sp_search_sprint_list(IN p_workspace_id BIGINT, IN p_like VARCHAR(255), IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT sprint_id AS id,
           name      AS title,
           goal      AS body
      FROM sprints
     WHERE workspace_id = p_workspace_id
       AND (LOWER(name) LIKE p_like ESCAPE '!'
            OR LOWER(COALESCE(goal, '')) LIKE p_like ESCAPE '!')
     ORDER BY sprint_id DESC
     LIMIT p_limit;
END //

DELIMITER ;


-- 댓글 검색 (작업 댓글 + 휴지통이 아닌 페이지의 블록 댓글)
--   p_workspace_id  워크스페이스 번호
--   p_like          LIKE 패턴 (소문자, '!'로 특수문자 이스케이프)
--   p_limit         최대 행 수
-- 결과: comment_id 역순 (id·title(작업 또는 페이지 이름)·body·meta(작성자 닉네임)·task_id·sprint_id·page_id)

DROP PROCEDURE IF EXISTS sp_search_comment_list;

DELIMITER //

CREATE PROCEDURE sp_search_comment_list(IN p_workspace_id BIGINT, IN p_like VARCHAR(255), IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT c.comment_id               AS id,
           COALESCE(t.title, p.title) AS title,
           c.content                  AS body,
           u.nickname                 AS meta,
           c.task_id,
           t.sprint_id,
           p.page_id
      FROM comments c
      JOIN users u       ON u.user_id = c.user_id
      LEFT JOIN tasks t  ON t.task_id = c.task_id
      LEFT JOIN blocks b ON b.block_id = c.block_id
      LEFT JOIN pages p  ON p.page_id = b.page_id
     WHERE (t.workspace_id = p_workspace_id OR p.workspace_id = p_workspace_id)
       AND (p.page_id IS NULL OR p.is_deleted = FALSE)
       AND LOWER(c.content) LIKE p_like ESCAPE '!'
     ORDER BY c.comment_id DESC
     LIMIT p_limit;
END //

DELIMITER ;


-- 일정 검색 (제목·설명)
--   p_workspace_id  워크스페이스 번호
--   p_like          LIKE 패턴 (소문자, '!'로 특수문자 이스케이프)
--   p_limit         최대 행 수
-- 결과: 시작 시각 역순 (id·title·body·start_datetime)

DROP PROCEDURE IF EXISTS sp_search_event_list;

DELIMITER //

CREATE PROCEDURE sp_search_event_list(IN p_workspace_id BIGINT, IN p_like VARCHAR(255), IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT event_id    AS id,
           title,
           description AS body,
           start_datetime
      FROM events
     WHERE workspace_id = p_workspace_id
       AND (LOWER(title) LIKE p_like ESCAPE '!'
            OR LOWER(COALESCE(description, '')) LIKE p_like ESCAPE '!')
     ORDER BY start_datetime DESC
     LIMIT p_limit;
END //

DELIMITER ;
