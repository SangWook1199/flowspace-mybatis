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
