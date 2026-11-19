-- 댓글 프로시저 (Comment_SQL.xml, Mapper 메서드 순서)
-- 이 파일만 다시 적용: docker compose exec mysql mysql -uroot -p flow_space -e "source /database/procedures/comment.sql"


-- 이메일로 사용자 번호와 닉네임
--   p_email  이메일
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_user_name_by_email;

DELIMITER //

CREATE PROCEDURE sp_user_name_by_email(IN p_email VARCHAR(100))
    READS SQL DATA
BEGIN
    SELECT user_id,
           nickname
    FROM users
    WHERE email = p_email;
END //

DELIMITER ;


-- 댓글 한 건
--   p_comment_id  댓글 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_comment;

DELIMITER //

CREATE PROCEDURE sp_comment(IN p_comment_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT c.comment_id,
           c.task_id,
           c.block_id,
           c.user_id,
           c.parent_comment_id,
           c.content,
           c.created_at,
           c.updated_at,
           u.nickname AS user_name,
           u.profile_file_id,
           f.file_url AS profile_image_url
      FROM comments c
      JOIN users u ON u.user_id = c.user_id
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE c.comment_id = p_comment_id;
END //

DELIMITER ;


-- 여러 작업의 댓글 (원 댓글·답글 함께 조회)
--   p_task_ids  작업 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_comment_list_by_tasks;

DELIMITER //

CREATE PROCEDURE sp_comment_list_by_tasks(IN p_task_ids JSON)
    READS SQL DATA
BEGIN
    SELECT c.comment_id,
           c.task_id,
           c.block_id,
           c.user_id,
           c.parent_comment_id,
           c.content,
           c.created_at,
           c.updated_at,
           u.nickname AS user_name,
           u.profile_file_id,
           f.file_url AS profile_image_url
      FROM comments c
      JOIN users u ON u.user_id = c.user_id
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE c.task_id IN (SELECT id FROM JSON_TABLE(p_task_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids)
     ORDER BY c.created_at, c.comment_id;
END //

DELIMITER ;


-- 여러 블록의 댓글
--   p_block_ids  블록 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_comment_list_by_blocks;

DELIMITER //

CREATE PROCEDURE sp_comment_list_by_blocks(IN p_block_ids JSON)
    READS SQL DATA
BEGIN
    SELECT c.comment_id,
           c.task_id,
           c.block_id,
           c.user_id,
           c.parent_comment_id,
           c.content,
           c.created_at,
           c.updated_at,
           u.nickname AS user_name,
           u.profile_file_id,
           f.file_url AS profile_image_url
      FROM comments c
      JOIN users u ON u.user_id = c.user_id
      LEFT JOIN files f ON f.file_id = u.profile_file_id
     WHERE c.block_id IN (SELECT id FROM JSON_TABLE(p_block_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids)
     ORDER BY c.created_at, c.comment_id;
END //

DELIMITER ;


-- 댓글 등록 (taskId, blockId 중 하나)
--   p_task_id            작업 번호 (블록 댓글이면 NULL)
--   p_block_id           블록 번호 (작업 댓글이면 NULL)
--   p_user_id            작성자 번호
--   p_parent_comment_id  원 댓글 번호 (답글이 아니면 NULL)
--   p_content            내용
--   p_comment_id         새 번호
-- 결과: 없음 (p_comment_id에 새 번호)

DROP PROCEDURE IF EXISTS sp_comment_insert;

DELIMITER //

CREATE PROCEDURE sp_comment_insert(IN p_task_id BIGINT, IN p_block_id BIGINT, IN p_user_id BIGINT,
                                   IN p_parent_comment_id BIGINT, IN p_content TEXT, OUT p_comment_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    INSERT INTO comments (task_id, block_id, user_id, parent_comment_id, content)
    VALUES (p_task_id, p_block_id, p_user_id, p_parent_comment_id, p_content);

    SET p_comment_id = LAST_INSERT_ID();
END //

DELIMITER ;


-- 댓글 내용 수정
--   p_content     내용
--   p_comment_id  댓글 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_comment_content_update;

DELIMITER //

CREATE PROCEDURE sp_comment_content_update(IN p_content TEXT, IN p_comment_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    UPDATE comments
    SET content = p_content
    WHERE comment_id = p_comment_id;
END //

DELIMITER ;


-- 댓글 삭제 (답글은 ON DELETE CASCADE)
--   p_comment_id  댓글 번호
-- 결과: 없음

DROP PROCEDURE IF EXISTS sp_comment_delete;

DELIMITER //

CREATE PROCEDURE sp_comment_delete(IN p_comment_id BIGINT)
    MODIFIES SQL DATA
BEGIN
    DELETE FROM comments
    WHERE comment_id = p_comment_id;
END //

DELIMITER ;


-- 작업 정보
--   p_task_id  작업 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_comment_task;

DELIMITER //

CREATE PROCEDURE sp_comment_task(IN p_task_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT task_id AS target_id,
           workspace_id,
           title,
           created_by,
           sprint_id
    FROM tasks
    WHERE task_id = p_task_id;
END //

DELIMITER ;


-- 블록 댓글용 페이지 정보 (워크스페이스, 제목, 작성자, 휴지통 여부)
--   p_block_id  블록 번호
-- 결과: 0행 또는 1행

DROP PROCEDURE IF EXISTS sp_comment_block;

DELIMITER //

CREATE PROCEDURE sp_comment_block(IN p_block_id BIGINT)
    READS SQL DATA
BEGIN
    SELECT p.page_id      AS target_id,
           p.workspace_id,
           p.title,
           p.created_by,
           p.is_deleted   AS page_deleted
      FROM blocks b
      JOIN pages p ON p.page_id = b.page_id
     WHERE b.block_id = p_block_id;
END //

DELIMITER ;


-- 멘션된 사용자 중 워크스페이스 멤버
--   p_workspace_id  워크스페이스 번호
--   p_user_ids      사용자 번호 목록 (JSON 배열)
-- 결과: 여러 행

DROP PROCEDURE IF EXISTS sp_workspace_member_id_list;

DELIMITER //

CREATE PROCEDURE sp_workspace_member_id_list(IN p_workspace_id BIGINT, IN p_user_ids JSON)
    READS SQL DATA
BEGIN
    SELECT user_id
    FROM workspace_members
    WHERE workspace_id = p_workspace_id
      AND user_id IN (SELECT id FROM JSON_TABLE(p_user_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) AS ids);
END //

DELIMITER ;
