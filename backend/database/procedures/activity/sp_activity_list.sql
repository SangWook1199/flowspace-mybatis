-- 워크스페이스 활동 기록을, 한 사람과 대상의 지금 이름까지 함께 최신순으로 조회해요.
-- 대상(작업·스프린트·페이지·댓글)은 종류마다 다른 테이블에 있어서 종류별로 LEFT JOIN 해요.
-- 대상이 지워졌으면 그 컬럼들이 NULL로 나와요(자바에서 이름 없이 보여줘요).
--   인자: p_workspace_id  워크스페이스 번호
--         p_user_id       한 사람의 활동만 볼 때 그 사람 번호 (NULL이면 모두)
--         p_types         보고 싶은 활동 종류를 쉼표로 이은 문자열, 예: 'TASK_CREATED,TASK_COMPLETED' (NULL이면 모두)
--                         프로시저는 목록을 인자로 받기 어려워서 문자열로 받고 FIND_IN_SET으로 찾아요.
--         p_offset        건너뛸 개수
--         p_limit         가져올 개수

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
           -- 대상이 작업일 때
           t.title           AS task_title,
           t.sprint_id       AS task_sprint_id,
           -- 대상이 스프린트일 때
           s.name            AS sprint_name,
           -- 대상이 페이지일 때
           p.title           AS page_title,
           p.is_deleted      AS page_deleted,
           -- 대상이 댓글일 때: 댓글이 달린 작업 또는 페이지
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
