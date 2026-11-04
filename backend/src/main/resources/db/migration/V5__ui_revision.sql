-- V5: theo CELEHUB_SCREEN_FLOWS.md (thông báo 2 loại, bài tập có/không chấm, quiz làm 1 lần + resume)
ALTER TABLE notifications
  ADD COLUMN type      VARCHAR(10) NOT NULL DEFAULT 'CLASS' CHECK (type IN ('GENERAL','CLASS')),
  ADD COLUMN course_id BIGINT REFERENCES courses(id) ON DELETE SET NULL,
  ADD COLUMN image_url VARCHAR(500),
  ADD COLUMN ref_type  VARCHAR(20),   -- ASSIGNMENT | QUIZ | LESSON | TUITION | MATERIAL
  ADD COLUMN ref_id    BIGINT;
CREATE INDEX idx_notifications_user_type ON notifications(user_id, type, is_read);

ALTER TABLE assignments ADD COLUMN graded BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE quiz_attempts
  ADD COLUMN ends_at        TIMESTAMP,
  ADD COLUMN exit_count     INT     NOT NULL DEFAULT 0,
  ADD COLUMN auto_submitted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_quiz_attempts_open ON quiz_attempts(ends_at) WHERE submitted_at IS NULL;
CREATE INDEX idx_questions_quiz ON questions(quiz_id, order_index);
CREATE INDEX idx_options_question ON question_options(question_id);
