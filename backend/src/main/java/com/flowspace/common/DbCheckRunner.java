package com.flowspace.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// 1단계 확인용 임시 코드예요. 서버가 켜질 때 MyBatis로 쿼리를 한 번 실행해 로그에 남겨요(2단계가 끝나면 지워요).
@Component
@RequiredArgsConstructor
public class DbCheckRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DbCheckRunner.class);

    private final DbCheckMapper dbCheckMapper;

    @Override
    public void run(ApplicationArguments args) {
        log.info("MyBatis check: task_statuses rows = {}", dbCheckMapper.countTaskStatuses());
    }
}
