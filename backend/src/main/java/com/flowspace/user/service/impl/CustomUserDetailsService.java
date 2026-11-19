package com.flowspace.user.service.impl;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.flowspace.user.service.UserVO;

import lombok.RequiredArgsConstructor;

// 로그인 사용자 조회 (Spring Security, 탈퇴한 사용자는 비활성)
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // 사용자 SQL
    private final UserMapper userMapper;

    // 이메일로 로그인 사용자 조회
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        UserVO user = userMapper.selectUserByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        return User.withUsername(user.getEmail())
            .password(user.getPassword() == null ? "" : user.getPassword())
            .authorities("ROLE_USER")
            .disabled(user.getDeletedAt() != null)
            .build();
    }
}
