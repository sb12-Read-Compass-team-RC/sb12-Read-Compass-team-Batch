package com.rc.readcompassbatch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 시작 시 특정 잡을 1회 실행한다. 두 가지 용도:
 *  1) 로컬 테스트: --batch.job=rankingJob 또는 application.yml 의 batch.run-on-startup=true
 *  2) ECS Scheduled Task: 컨테이너가 잡을 수행하고 종료하는 배포 방식
 *
 * 실행 우선순위: 커맨드라인 인자(--batch.job=...) > 프로퍼티(batch.run-on-startup)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupJobRunner implements ApplicationRunner {

  private final BatchJobLauncher launcher;

  /** 시작 시 모든 잡을 1회 실행할지 여부 (application.yml: batch.run-on-startup). */
  @Value("${batch.run-on-startup:false}")
  private boolean runOnStartup;

  @Override
  public void run(ApplicationArguments args) {
    // 1) --batch.job=rankingJob 같은 명시적 실행 (인자가 프로퍼티보다 우선)
    if (args.containsOption("batch.job")) {
      args.getOptionValues("batch.job").forEach(launcher::runByName);
      return;
    }
    // 2) batch.run-on-startup=true 면 두 잡을 모두 1회 실행 (로컬 검증용)
    if (runOnStartup) {
      log.info("[Startup] run-on-startup enabled - running all jobs once");
      launcher.runRanking();
      launcher.runMaintenance();
    }
  }
}