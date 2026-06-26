package com.rc.readcompassbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 덕후감 배치 애플리케이션 (read-compass-batch).
 * 메인 프로젝트(read-compass)와 동일한 RDB 를 공유하며,
 * 랭킹 스냅샷 적재와 정리(알림/유저) 배치만 담당한다.
 */
@EnableScheduling
@SpringBootApplication
public class ReadCompassBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadCompassBatchApplication.class, args);
    }
}
