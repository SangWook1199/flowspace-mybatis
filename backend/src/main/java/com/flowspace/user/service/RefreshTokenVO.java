package com.flowspace.user.service;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// refresh_tokens 테이블 한 행 (계정당 하나)
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenVO {

    private Long userId;

    private String token;

    private LocalDateTime expiredAt;
}
