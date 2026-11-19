package com.flowspace.user.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 이메일로 찾은 사용자 (sp_user_lookup 결과)
@Getter
@Setter
@NoArgsConstructor
public class UserLookupVO {

    private Long userId;

    private String nickname;

    private String email;

    private String profileImageUrl;

    // 워크스페이스 멤버 여부 (Boolean 타입 사용: is_member → setIsMember 매핑)
    private Boolean isMember;

    // 그 워크스페이스에서 이미 초대했는지 (대기 중)
    private Boolean isInvited;
}
