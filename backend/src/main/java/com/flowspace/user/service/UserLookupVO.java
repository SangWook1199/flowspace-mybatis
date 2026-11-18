package com.flowspace.user.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 이메일로 찾은 사용자 한 명이에요(프로시저 sp_user_lookup의 결과).
@Getter
@Setter
@NoArgsConstructor
public class UserLookupVO {

    private Long userId;

    private String nickname;

    private String email;

    private String profileImageUrl;

    // 그 워크스페이스에 이미 멤버인지 (boolean이 아니라 Boolean이어야 is_member가 setIsMember로 채워져요)
    private Boolean isMember;

    // 그 워크스페이스에서 이미 초대했는지 (대기 중)
    private Boolean isInvited;
}
