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
