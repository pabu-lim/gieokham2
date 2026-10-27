-- 기억함2 초기 테이블 구조
-- PostgreSQL 17
-- 날짜·시간은 한국 시간 기준의 TIMESTAMP로 저장한다.
-- 빈 데이터베이스에서 실행한다.

BEGIN;

CREATE TABLE category (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
               DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),
    updated_at TIMESTAMP NOT NULL
               DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),

    CONSTRAINT ck_category_name_not_blank
        CHECK (name ~ '[^[:space:]]'),

    CONSTRAINT ck_category_sort_order_non_negative
        CHECK (sort_order >= 0)
);

CREATE TABLE memo (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    content      VARCHAR(10000) NOT NULL,
    priority     VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    category_id  BIGINT,
    completed_at TIMESTAMP,
    created_at   TIMESTAMP NOT NULL
                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),
    updated_at   TIMESTAMP NOT NULL
                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),

    CONSTRAINT ck_memo_content_not_blank
        CHECK (content ~ '[^[:space:]]'),

    CONSTRAINT ck_memo_priority
        CHECK (priority IN (
            'HIGHEST',
            'HIGH',
            'MEDIUM',
            'LOW',
            'LOWEST'
        )),

    CONSTRAINT fk_memo_category
        FOREIGN KEY (category_id)
        REFERENCES category (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_memo_category_id
    ON memo (category_id);

COMMIT;