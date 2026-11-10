-- V3: schema extensions for modules 1-11
-- Enum columns are stored as VARCHAR for JPA portability; CHECK constraints keep integrity.

ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(32) USING role::text;
ALTER TABLE student_profiles ALTER COLUMN membership_status DROP DEFAULT;
ALTER TABLE student_profiles ALTER COLUMN membership_status TYPE VARCHAR(32) USING membership_status::text;
ALTER TABLE student_profiles ALTER COLUMN membership_status SET DEFAULT 'PENDING';
ALTER TABLE submissions ALTER COLUMN status DROP DEFAULT;
ALTER TABLE submissions ALTER COLUMN status TYPE VARCHAR(32) USING status::text;
ALTER TABLE submissions ALTER COLUMN status SET DEFAULT 'IN_PROGRESS';
ALTER TABLE activities ALTER COLUMN type TYPE VARCHAR(32) USING type::text;
ALTER TABLE point_ledger ALTER COLUMN currency TYPE VARCHAR(16) USING currency::text;
ALTER TABLE award_feed_items DROP CONSTRAINT award_feed_items_currency_check;
ALTER TABLE award_feed_items ALTER COLUMN currency TYPE VARCHAR(16) USING currency::text;
ALTER TABLE poll_questions ALTER COLUMN type TYPE VARCHAR(16) USING type::text;

DROP TYPE user_role;
DROP TYPE membership_status;
DROP TYPE submission_status;
DROP TYPE activity_type;
DROP TYPE point_currency;
DROP TYPE poll_question_type;

ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN'));
ALTER TABLE student_profiles ADD CONSTRAINT chk_student_membership
    CHECK (membership_status IN ('PENDING', 'CONFIRMED', 'REJECTED'));
ALTER TABLE submissions ADD CONSTRAINT chk_submission_status
    CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'OVERDUE', 'ACCEPTED', 'RETURNED'));
ALTER TABLE activities ADD CONSTRAINT chk_activity_type CHECK (type IN ('ACADEMIC', 'SOCIAL'));
ALTER TABLE point_ledger ADD CONSTRAINT chk_ledger_currency CHECK (currency IN ('AR', 'BUA', 'BOA'));
ALTER TABLE award_feed_items ADD CONSTRAINT chk_award_feed_currency CHECK (currency IN ('BUA', 'BOA'));
ALTER TABLE poll_questions ADD CONSTRAINT chk_poll_question_type CHECK (type IN ('SINGLE', 'MULTIPLE'));

-- Membership confirmation audit
ALTER TABLE student_profiles
    ADD COLUMN membership_confirmed_by BIGINT REFERENCES users(id),
    ADD COLUMN membership_confirmed_at TIMESTAMPTZ,
    ADD COLUMN enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Submission review details
ALTER TABLE submissions
    ADD COLUMN file_name VARCHAR(255),
    ADD COLUMN return_comment VARCHAR(2000);

-- Personal feed: human-readable rendered message
ALTER TABLE feed_events ADD COLUMN message VARCHAR(1000) NOT NULL DEFAULT '';

-- Notifications
ALTER TABLE notifications
    ADD COLUMN kind VARCHAR(64),
    ADD COLUMN link VARCHAR(255);

-- Poll end processing marker
ALTER TABLE polls ADD COLUMN ended_notified BOOLEAN NOT NULL DEFAULT FALSE;

-- Deadline reminder dedupe: one per student/checkpoint (spec 12 / PF-02)
CREATE TABLE deadline_reminders (
    id                  BIGSERIAL PRIMARY KEY,
    student_profile_id  BIGINT NOT NULL REFERENCES student_profiles(id) ON DELETE CASCADE,
    checkpoint_id       BIGINT NOT NULL REFERENCES checkpoints(id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (student_profile_id, checkpoint_id)
);

CREATE INDEX idx_submissions_student ON submissions (student_profile_id, status);
CREATE INDEX idx_ledger_ref ON point_ledger (ref_type, ref_id);
CREATE INDEX idx_notifications_user_time ON notifications (user_id, created_at DESC);
CREATE INDEX idx_polls_ends ON polls (ends_at);
CREATE INDEX idx_checkpoints_deadline ON checkpoints (deadline);
CREATE INDEX idx_feed_events_user_id ON feed_events (user_id, id DESC);
CREATE INDEX idx_award_feed_id ON award_feed_items (id DESC);
