package com.flowspace.presence.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.flowspace.page.service.PageVO;

// 접속 상태 Mapper (Presence_SQL.xml)
@Mapper
public interface PresenceMapper {

    // 이메일로 사용자 번호 (없으면 null)
    Long selectUserIdByEmail(String email);

    // 마지막 접속 시각 갱신
    int updateLastActiveAt(@Param("userId") Long userId, @Param("lastActiveAt") LocalDateTime lastActiveAt);

    // 같은 워크스페이스에 있는 다른 사용자 번호 (중복 제외)
    List<Long> selectCoMemberIds(Long userId);

    // 페이지의 워크스페이스·휴지통 여부 (없으면 null)
    PageVO selectPage(Long pageId);

    // 워크스페이스 멤버 수 (멤버면 1)
    int selectMemberCount(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId);
}
