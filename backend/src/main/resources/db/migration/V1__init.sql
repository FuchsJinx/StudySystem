-- UniRate schema (PostgreSQL 16)

CREATE TABLE faculties (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE departments (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE groups (
    id              BIGSERIAL PRIMARY KEY,
    faculty_id      BIGINT NOT NULL REFERENCES faculties(id),
    name            VARCHAR(64) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (faculty_id, name)
);

CREATE TYPE user_role AS ENUM ('STUDENT', 'TEACHER', 'ADMIN');
CREATE TYPE membership_status AS ENUM ('PENDING', 'CONFIRMED', 'REJECTED');
CREATE TYPE submission_status AS ENUM ('IN_PROGRESS', 'SUBMITTED', 'OVERDUE', 'ACCEPTED', 'RETURNED');
CREATE TYPE activity_type AS ENUM ('ACADEMIC', 'SOCIAL');
CREATE TYPE point_currency AS ENUM ('AR', 'BUA', 'BOA');
CREATE TYPE poll_question_type AS ENUM ('SINGLE', 'MULTIPLE');

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(320) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            user_role NOT NULL,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE student_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    group_id            BIGINT NOT NULL REFERENCES groups(id),
    membership_status   membership_status NOT NULL DEFAULT 'PENDING',
    is_group_lead       BOOLEAN NOT NULL DEFAULT FALSE,
    academic_rating     INT NOT NULL DEFAULT 1000
        CHECK (academic_rating >= 0 AND academic_rating <= 1000)
);

CREATE TABLE teacher_profiles (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    department_id   BIGINT NOT NULL REFERENCES departments(id)
);

CREATE TABLE teacher_group_links (
    teacher_profile_id  BIGINT NOT NULL REFERENCES teacher_profiles(id) ON DELETE CASCADE,
    group_id            BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (teacher_profile_id, group_id)
);

CREATE TABLE email_verification_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ
);

CREATE TABLE checkpoints (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL DEFAULT '',
    deadline        TIMESTAMPTZ NOT NULL,
    cost            INT NOT NULL CHECK (cost > 0),
    created_by      BIGINT NOT NULL REFERENCES teacher_profiles(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE checkpoint_groups (
    checkpoint_id   BIGINT NOT NULL REFERENCES checkpoints(id) ON DELETE CASCADE,
    group_id        BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (checkpoint_id, group_id)
);

CREATE TABLE submissions (
    id                  BIGSERIAL PRIMARY KEY,
    checkpoint_id       BIGINT NOT NULL REFERENCES checkpoints(id) ON DELETE CASCADE,
    student_profile_id  BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    status              submission_status NOT NULL DEFAULT 'IN_PROGRESS',
    submission_url      VARCHAR(2048),
    file_path           VARCHAR(1024),
    ar_debited          INT NOT NULL DEFAULT 0 CHECK (ar_debited >= 0),
    submitted_at        TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (checkpoint_id, student_profile_id)
);

CREATE INDEX idx_submissions_job ON submissions (status, checkpoint_id);

CREATE TABLE activities (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL DEFAULT '',
    type            activity_type NOT NULL,
    event_date      DATE NOT NULL,
    max_points      INT NOT NULL CHECK (max_points > 0),
    created_by      BIGINT NOT NULL REFERENCES teacher_profiles(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE activity_groups (
    activity_id BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    group_id    BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (activity_id, group_id)
);

CREATE TABLE activity_enrollments (
    id                  BIGSERIAL PRIMARY KEY,
    activity_id         BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    student_profile_id  BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    enrolled_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (activity_id, student_profile_id)
);

CREATE TABLE activity_awards (
    id                  BIGSERIAL PRIMARY KEY,
    activity_id         BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    student_profile_id  BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    level_percent       INT NOT NULL CHECK (level_percent IN (25, 50, 75, 100)),
    points              INT NOT NULL CHECK (points >= 0),
    awarded_by          BIGINT NOT NULL REFERENCES teacher_profiles(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (activity_id, student_profile_id)
);

CREATE TABLE point_ledger (
    id                  BIGSERIAL PRIMARY KEY,
    student_profile_id  BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    currency            point_currency NOT NULL,
    amount              INT NOT NULL,
    reason              VARCHAR(512) NOT NULL,
    ref_type            VARCHAR(64),
    ref_id              BIGINT,
    actor_user_id       BIGINT REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_student_time ON point_ledger (student_profile_id, created_at DESC);

CREATE TABLE budget_spends (
    id                  BIGSERIAL PRIMARY KEY,
    teacher_profile_id  BIGINT NOT NULL REFERENCES teacher_profiles(id) ON DELETE CASCADE,
    group_id            BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    year_month          CHAR(7) NOT NULL,
    amount              INT NOT NULL DEFAULT 0 CHECK (amount >= 0),
    UNIQUE (teacher_profile_id, group_id, year_month)
);

CREATE TABLE polls (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     TEXT NOT NULL DEFAULT '',
    ends_at         TIMESTAMPTZ NOT NULL,
    is_anonymous    BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      BIGINT NOT NULL REFERENCES teacher_profiles(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE poll_groups (
    poll_id     BIGINT NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    group_id    BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (poll_id, group_id)
);

CREATE TABLE poll_questions (
    id          BIGSERIAL PRIMARY KEY,
    poll_id     BIGINT NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    text        VARCHAR(1024) NOT NULL,
    type        poll_question_type NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE poll_options (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES poll_questions(id) ON DELETE CASCADE,
    text        VARCHAR(512) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE poll_responses (
    id                  BIGSERIAL PRIMARY KEY,
    poll_id             BIGINT NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    student_profile_id  BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (poll_id, student_profile_id)
);

CREATE TABLE poll_response_options (
    response_id BIGINT NOT NULL REFERENCES poll_responses(id) ON DELETE CASCADE,
    option_id   BIGINT NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    PRIMARY KEY (response_id, option_id)
);

CREATE TABLE award_feed_items (
    id                  BIGSERIAL PRIMARY KEY,
    student_profile_id  BIGINT NOT NULL REFERENCES student_profiles(id),
    currency            point_currency NOT NULL CHECK (currency IN ('BUA', 'BOA')),
    amount              INT NOT NULL,
    activity_title      VARCHAR(255) NOT NULL,
    teacher_name        VARCHAR(255) NOT NULL,
    award_id            BIGINT REFERENCES activity_awards(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_award_feed_time ON award_feed_items (created_at DESC);

CREATE TABLE feed_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_type  VARCHAR(64) NOT NULL,
    payload     JSONB NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_feed_events_user ON feed_events (user_id, created_at DESC);

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    body        TEXT NOT NULL,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notification_settings (
    user_id             BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    email_enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    telegram_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    telegram_chat_id    VARCHAR(64)
);

CREATE TABLE app_settings (
    key         VARCHAR(128) PRIMARY KEY,
    value       VARCHAR(512) NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE semesters (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    start_at        DATE NOT NULL,
    end_at          DATE NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    activated_at    TIMESTAMPTZ
);
