package com.flowspace.retrospective.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회고 페이지의 TODO 블록 (체크 여부는 content JSON의 checked)
@Getter
@Setter
@NoArgsConstructor
public class TodoBlockVO {

    private Long pageId;

    private String content;
}
