-- 블록 댓글을 달 때 필요한, 그 블록이 있는 페이지 정보를 조회해요.
-- (어느 워크스페이스인지, 알림에 쓸 제목·만든 사람, 휴지통에 있는지)
--   인자: p_block_id  블록 번호
--   결과: 한 행 (없으면 0행)

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
