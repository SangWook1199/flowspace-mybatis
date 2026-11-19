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
