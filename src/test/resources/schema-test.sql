-- 배치 단독 검증용 스키마 (H2, PostgreSQL 호환 모드).
-- 메인 프로젝트의 schema.sql 중 배치가 참조/적재하는 테이블만 추린 것이다.

CREATE TABLE IF NOT EXISTS tb_users (
    id          UUID PRIMARY KEY,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP WITH TIME ZONE NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_books (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_reviews (
    id          UUID PRIMARY KEY,
    book_id     UUID NOT NULL,
    user_id     UUID NOT NULL,
    rating      INTEGER NOT NULL,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_review_likes (
    id          UUID PRIMARY KEY,
    review_id   UUID NOT NULL,
    user_id     UUID NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_comments (
    id          UUID PRIMARY KEY,
    review_id   UUID NOT NULL,
    user_id     UUID NOT NULL,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_notifications (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    review_id   UUID NOT NULL,
    message     TEXT NOT NULL,
    noti_type   VARCHAR(30) NOT NULL,
    confirmed   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_book_rankings (
    id            UUID PRIMARY KEY,
    book_id       UUID NOT NULL,
    period_type   VARCHAR(20) NOT NULL,
    rank_position INTEGER NOT NULL,
    score         DECIMAL(10,2) NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_review_rankings (
    id            UUID PRIMARY KEY,
    review_id     UUID NOT NULL,
    period_type   VARCHAR(20) NOT NULL,
    rank_position INTEGER NOT NULL,
    score         DECIMAL(10,2) NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_user_rankings (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL,
    period_type   VARCHAR(20) NOT NULL,
    rank_position INTEGER NOT NULL,
    score         DECIMAL(10,2) NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
