package com.flowspace.page.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 페이지 한 개예요. 등록할 때와 조회할 때(sp_page_list, sp_page, sp_page_subtree) 같이 써요.
@Getter
@Setter
@NoArgsConstructor
public class PageVO {

    private Long pageId;

    private Long workspaceId;

    // 상위 페이지 (최상위면 null)
    private Long parentPageId;

    private String title;

    private String icon;

    private Long coverFileId;

    // 커버 이미지 주소 (files 조인)
    private String coverUrl;

    // 같은 상위 페이지 안에서의 순서
    private Integer position;

    private Long createdBy;

    private Boolean isDeleted;

    private LocalDateTime deletedAt;

    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 회고 페이지인지
    private Boolean retrospective;

    // 상위 페이지가 휴지통에 있는지 (상위가 없으면 null)
    private Boolean parentDeleted;

    // 하위 페이지 조회(sp_page_subtree)에서만: 본인 0, 자식 1, 손자 2 …
    private Integer depth;
}
