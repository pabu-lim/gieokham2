-- 기억함2 초기 테이블 구조
-- PostgreSQL 17
-- 날짜·시간은 한국 시간 기준 TIMESTAMP 사용
-- 빈 데이터베이스에서 실행

CREATE TABLE member (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    nickname   VARCHAR(100) NOT NULL,
    email      VARCHAR(320),
    created_at TIMESTAMP NOT NULL
               DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),
    updated_at TIMESTAMP NOT NULL
               DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),

    CONSTRAINT ck_member_name_not_blank
        CHECK (name ~ '[^[:space:]]'),

    CONSTRAINT ck_member_nickname_not_blank
        CHECK (nickname ~ '[^[:space:]]')
);

CREATE TABLE member_identity (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id        BIGINT NOT NULL,
    provider         VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email            VARCHAR(320),
    created_at       TIMESTAMP NOT NULL
                     DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),
    updated_at       TIMESTAMP NOT NULL
                     DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),

    CONSTRAINT fk_member_identity_member
        FOREIGN KEY (member_id) REFERENCES member(id),

    CONSTRAINT uq_member_identity_provider_account
        UNIQUE (provider, provider_user_id),

    CONSTRAINT ck_member_identity_provider_not_blank
        CHECK (provider ~ '[^[:space:]]'),

    CONSTRAINT ck_member_identity_provider_user_id_not_blank
        CHECK (provider_user_id ~ '[^[:space:]]')
);

CREATE TABLE category (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id  BIGINT NOT NULL,
    parent_id BIGINT,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
               DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),
    updated_at TIMESTAMP NOT NULL
               DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),

    CONSTRAINT fk_category_member
        FOREIGN KEY (member_id) REFERENCES member(id),

    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id)
        REFERENCES category(id)
        ON DELETE SET NULL,

    CONSTRAINT ck_category_parent_not_self
        CHECK (parent_id <> id),

    CONSTRAINT ck_category_name_not_blank
        CHECK (name ~ '[^[:space:]]'),

    CONSTRAINT ck_category_sort_order_non_negative
        CHECK (sort_order >= 0)
);

CREATE TABLE memo (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id    BIGINT NOT NULL,
    content      VARCHAR(10000) NOT NULL,
    notes        VARCHAR(10000),
    priority     VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    category_id  BIGINT,
    start_date DATE,
    end_date   DATE,
    start_time TIME,
    end_time   TIME,
    repeat_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
    repeat_day_of_month INTEGER,
    repeat_month        INTEGER,
    completed_at TIMESTAMP,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL
                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),
    updated_at   TIMESTAMP NOT NULL
                 DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),

    CONSTRAINT fk_memo_member
        FOREIGN KEY (member_id) REFERENCES member(id),

    CONSTRAINT fk_memo_category
        FOREIGN KEY (category_id) REFERENCES category(id)
        ON DELETE SET NULL,

    CONSTRAINT ck_memo_content_not_blank
        CHECK (content ~ '[^[:space:]]'),

    CONSTRAINT ck_memo_priority
        CHECK (priority IN (
            'HIGHEST', 'HIGH', 'MEDIUM', 'LOW', 'LOWEST'
        ))
);

CREATE TABLE memo_repeat_day (
    memo_id     BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,

    PRIMARY KEY (memo_id, day_of_week),

    CONSTRAINT fk_memo_repeat_day_memo
        FOREIGN KEY (memo_id) REFERENCES memo(id)
        ON DELETE CASCADE
);

CREATE TABLE memo_completion (
    memo_id        BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    completed_at   TIMESTAMP NOT NULL
                   DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'),

    PRIMARY KEY (memo_id, scheduled_date),

    CONSTRAINT fk_memo_completion_memo
        FOREIGN KEY (memo_id) REFERENCES memo(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_member_identity_member_id
    ON member_identity(member_id);

CREATE INDEX idx_category_member_id
    ON category(member_id);

CREATE INDEX idx_category_parent_id
    ON category(parent_id);

CREATE INDEX idx_memo_member_id
    ON memo(member_id);

CREATE INDEX idx_memo_category_id
    ON memo(category_id);