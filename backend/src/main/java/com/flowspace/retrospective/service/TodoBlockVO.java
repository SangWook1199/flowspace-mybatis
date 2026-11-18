package com.flowspace.retrospective.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회고 페이지의 TODO 블록 하나예요. 체크 여부는 content(JSON)의 checked 값으로 자바에서 판단해요.
@Getter
@Setter
@NoArgsConstructor
public class TodoBlockVO {

    private Long pageId;

    private String content;
}
