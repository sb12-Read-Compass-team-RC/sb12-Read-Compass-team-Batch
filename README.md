# read-compass-batch

덕후감(read-compass) 메인 프로젝트의 **랭킹 산출 / 유지보수 배치 전용** 프로젝트입니다.
메인 프로젝트와 **소스를 공유하지 않고**, 같은 RDB(PostgreSQL)만 공유하는 **독립 실행 애플리케이션**입니다.

## 무엇을 하나

매일 1회 배치로 아래를 수행합니다.

**랭킹 산출 (`rankingJob`)** — 소스 테이블(`tb_reviews`, `tb_review_likes`, `tb_comments`, `tb_users`)을 읽어 기간별(DAILY/WEEKLY/MONTHLY/ALL_TIME) 점수를 계산하고, 스냅샷 행을 랭킹 테이블에 INSERT 합니다.

| 대상 | 테이블 | 점수 공식 |
|------|--------|-----------|
| 인기 도서 | `tb_book_rankings` | `기간 리뷰수 * 0.4 + 기간 평점평균 * 0.6` |
| 인기 리뷰 | `tb_review_rankings` | `기간 좋아요수 * 0.3 + 기간 댓글수 * 0.7` |
| 파워 유저 | `tb_user_rankings` | `작성 리뷰 인기점수합 * 0.5 + 참여 좋아요 * 0.2 + 참여 댓글 * 0.3` |

- 한 번의 실행에서 네 기간이 동일한 `calculated_at` 타임스탬프로 저장됩니다. 조회 API는 `period_type`별 최신 `calculated_at`을 읽으면 됩니다.
- 랭킹 산출 시 **논리 삭제된 리뷰/댓글도 포함**합니다
- 각 기간 TOP 10 리뷰 작성자에게 `REVIEW_RANKED` 알림을 생성합니다.

**유지보수 (`maintenanceJob`)**
- 확인(`confirmed=true`)된 지 7일 지난 알림 물리 삭제
- 논리 삭제(`is_deleted=true`)된 지 1일 지난 사용자 물리 삭제 (FK `ON DELETE CASCADE`로 하위 데이터 동반 삭제)

## 메인 프로젝트와의 관계

```
          ┌──────────────────┐         ┌──────────────────────┐
          │  메인 프로젝트     │  write  │                      │
          │ (read-compass)   │────────▶│   PostgreSQL (RDB)   │
          │  :8080           │◀────────│   tb_reviews 등      │
          └──────────────────┘  read   │   tb_*_rankings      │
                                       └──────────┬───────────┘
          ┌──────────────────┐                    │ read/write
          │ 배치 프로젝트      │  read/write         │
          │ (이 프로젝트)      │─────────────────────┘
          │  :8081           │
          └──────────────────┘
```

두 애플리케이션은 **별개의 프로세스**입니다. 메인이 8080, 배치가 8081을 쓰므로 한 머신에서 동시에 띄워도 충돌하지 않습니다. 메인의 `ddl-auto`가 `validate`라 스키마는 메인의 `schema.sql`이 소유하며, 배치는 그 테이블을 읽고/쓸 뿐 DDL을 만들지 않습니다. 단, Spring Batch 메타데이터 테이블(`BATCH_*`)만 배치가 최초 1회 생성합니다(`db/schema-batch-postgresql.sql`, `IF NOT EXISTS`로 멱등).

## 로컬 실행

```bash
# 메인과 같은 DB를 바라봄. 포트는 8081.
./gradlew bootRun --args='--spring.profiles.active=dev'

# 한 번만 즉시 실행하고 싶으면 (스케줄러 대기 없이)
./gradlew bootRun --args='--spring.profiles.active=dev --batch.run-on-startup=true'

# 특정 Job만 실행하고 종료 (ECS Scheduled Task와 동일한 동작)
./gradlew bootRun --args='--spring.profiles.active=dev --batch.job=rankingJob'
```

`application-dev.yml`은 `jdbc:postgresql://localhost:5432/readcompass`를 가리킵니다. 메인 프로젝트가 쓰는 DB와 같게 맞추세요.

## 테스트

메인을 띄울 필요가 **전혀 없습니다.** 테스트는 배치 컨텍스트만 H2(PostgreSQL 모드)로 올린 뒤 Job을 코드로 실행해 검증합니다.

```bash
./gradlew test
```

`src/test/java/.../RankingJobTest.java`가 `JobOperatorTestUtils`로 `rankingJob` / `maintenanceJob`을 직접 구동하고, 랭킹 행·알림 생성·알림 정리·사용자 물리삭제를 단언합니다. 테스트 데이터는 `src/test/resources/data-test.sql`.

## 배포 (AWS)

두 가지 패턴 중 택일.

1. **ECS Scheduled Task (권장, 비용 효율적)** — EventBridge 규칙이 매일 정해진 시각에 Task를 띄우고, `--batch.job=rankingJob` / `--batch.job=maintenanceJob`로 해당 Job만 실행 후 컨테이너가 종료됩니다. 상주 비용이 없습니다. 이때 앱 내부 `@Scheduled`는 끕니다(`batch.scheduler.enabled=false`, prod 기본값).
2. **ECS Service 상주 + 내부 스케줄러** — 컨테이너를 계속 띄워두고 `@Scheduled` 크론(랭킹 03:00, 유지보수 04:00 KST)으로 실행. 운영이 단순하지만 24시간 과금됩니다. `batch.scheduler.enabled=true`로 켭니다.

로그는 날짜별로 S3에 적재(심화 요구사항). `application-prod.yml`의 S3 placeholder를 Logback appender 또는 사이드카로 연결하세요. 커스텀 메트릭은 `/actuator/prometheus`로 노출됩니다.

## 빌드 검증 주의

이 코드는 Maven Central 접근이 불가능한 환경에서 작성되어 **`./gradlew build`로 컴파일 검증을 하지 못했습니다.** 받으신 뒤 반드시 로컬에서 `./gradlew build` 한 번 돌려 보세요.

## 스택

Spring Boot 4.1.0 · Spring Batch 6 · Java 17 · Gradle 9.5.1 · PostgreSQL · UUID v7

## 흐름

① 누가 깨우나        scheduler/  (3가지 방법)
│
② 실행 진입점        scheduler/BatchJobLauncher
│           JobOperator.start(job, 파라미터)
③ 잡 정의            batch/config/BatchConfig
│           rankingJob, maintenanceJob
④ 각 단계(Step)      batch/tasklet/  (얇은 껍데기)
│
⑤ 실제 로직 ★        service/  ← 여기가 핵심
│           RankingService, MaintenanceService
⑥ 쿼리               repository/
│
⑦ 공유 RDB           tb_reviews 읽기 → tb_*_rankings 쓰기