-- 활동 기록 프로시저 (Activity_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/activity.sql"


-- 활동 기록 등록 (생성 시각은 DB 기본값)
--   p_workspace_id  워크스페이스 번호
--   p_user_id       한 사람
--   p_type          활동 종류
--   p_target_type   대상 종류
--   p_target_id     대상 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_activity_insert;

DELIMITER //

CREATE PROCEDURE sp_activity_insert(IN p_workspace_id BIGINT, IN p_user_id BIGINT, IN p_type VARCHAR(50),
                                    IN p_target_type VARCHAR(20), IN p_target_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO activities (workspace_id, user_id, type, target_type, target_id)
    VALUES (p_workspace_id, p_user_id, p_type, p_target_type, p_target_id);
END //

DELIMITER ;


-- 워크스페이스 활동 기록 목록 (최신순, 사용자·대상 현재 이름 포함)
-- 대상 종류(작업·스프린트·페이지·댓글)별 LEFT JOIN, 삭제된 대상은 NULL
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호 (NULL이면 전체)
--   p_types         활동 종류 쉼표 구분 문자열 (예: 'TASK_CREATED,TASK_COMPLETED', NULL이면 전체, FIND_IN_SET 사용)
--   p_offset        건너뛸 개수
--   p_limit         가져올 개수

DROP PROCEDURE IF EXISTS sp_activity_list;

DELIMITER //

CREATE PROCEDURE sp_activity_list(IN p_workspace_id BIGINT, IN p_user_id BIGINT, IN p_types VARCHAR(200),
                                  IN p_offset INT, IN p_limit INT)
    READS SQL DATA
BEGIN
    SELECT a.activity_id,
           a.workspace_id,
           a.user_id,
           u.nickname        AS user_name,
           u.profile_file_id,
           f.file_url        AS profile_image_url,
           a.type,
           a.target_type,
           a.target_id,
           a.created_at,
           t.title           AS task_title,
           t.sprint_id       AS task_sprint_id,
           s.name            AS sprint_name,
           p.title           AS page_title,
           p.is_deleted      AS page_deleted,
           ct.task_id        AS comment_task_id,
           ct.title          AS comment_task_title,
           ct.sprint_id      AS comment_task_sprint_id,
           cp.page_id        AS comment_page_id,
           cp.title          AS comment_page_title,
           cp.is_deleted     AS comment_page_deleted
      FROM activities a
      JOIN users u           ON u.user_id = a.user_id
      LEFT JOIN files f      ON f.file_id = u.profile_file_id
      LEFT JOIN tasks t      ON a.target_type = 'TASK'    AND t.task_id = a.target_id
      LEFT JOIN sprints s    ON a.target_type = 'SPRINT'  AND s.sprint_id = a.target_id
      LEFT JOIN pages p      ON a.target_type = 'PAGE'    AND p.page_id = a.target_id
      LEFT JOIN comments c   ON a.target_type = 'COMMENT' AND c.comment_id = a.target_id
      LEFT JOIN tasks ct     ON ct.task_id = c.task_id
      LEFT JOIN blocks cb    ON cb.block_id = c.block_id
      LEFT JOIN pages cp     ON cp.page_id = cb.page_id
     WHERE a.workspace_id = p_workspace_id
       AND (p_user_id IS NULL OR a.user_id = p_user_id)
       AND (p_types IS NULL OR FIND_IN_SET(a.type, p_types) > 0)
     ORDER BY a.created_at DESC, a.activity_id DESC
     LIMIT p_limit OFFSET p_offset;
END //

DELIMITER ;


-- 활동 기록 수 (조건은 sp_activity_list와 동일)
--   p_workspace_id  워크스페이스 번호
--   p_user_id       사용자 번호 (NULL이면 전체)
--   p_types         활동 종류 쉼표 구분 문자열 (NULL이면 전체, FIND_IN_SET 사용)
-- 결과: 1행 (개수)

DROP PROCEDURE IF EXISTS sp_activity_count;

DELIMITER //

CREATE PROCEDURE sp_activity_count(IN p_workspace_id BIGINT, IN p_user_id BIGINT, IN p_types VARCHAR(200))
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
      FROM activities
     WHERE workspace_id = p_workspace_id
       AND (p_user_id IS NULL OR user_id = p_user_id)
       AND (p_types IS NULL OR FIND_IN_SET(type, p_types) > 0);
END //

DELIMITER ;


-- 사용자 존재 여부
--   p_user_id  사용자 번호
-- 결과: 1행 (있으면 1, 없으면 0)

DROP PROCEDURE IF EXISTS sp_user_count;

DELIMITER //

CREATE PROCEDURE sp_user_count(IN p_user_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT COUNT(*)
      FROM users
     WHERE user_id = p_user_id;
END //

DELIMITER ;
