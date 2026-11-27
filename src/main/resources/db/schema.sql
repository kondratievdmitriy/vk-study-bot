-- ============================================================
-- study-bot: схема базы данных (PostgreSQL 14+)
-- Задача 4: таблицы books, lessons, users
--
-- Применение:
--   psql -U postgres -d study_bot -f schema.sql
--   или в pgAdmin: Query Tool -> вставить -> F5.
-- Скрипт идемпотентен: повторный запуск ничего не ломает.
-- ============================================================

-- ── Книги: учебные материалы, общие для всех пользователей ──
-- Текст и структура хранятся отдельно от прогресса: одна книга
-- доступна многим пользователям (см. README, схема данных).
CREATE TABLE IF NOT EXISTS books (
    book_id       BIGSERIAL    PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    author        VARCHAR(200),
    total_lessons INT          NOT NULL DEFAULT 0,
    source_text   TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── Уроки: блоки, на которые книга разбита сплиттером ──
-- questions — JSON того же формата, что пишет QuestionCodec:
-- вопросы генерируются один раз при загрузке материала и
-- переиспользуются всеми пользователями этой книги.
-- Для уроков, где вопросов не сгенерировалось (текст слишком
-- короткий), поле остаётся NULL.
CREATE TABLE IF NOT EXISTS lessons (
    lesson_id     BIGSERIAL PRIMARY KEY,
    book_id       BIGINT    NOT NULL REFERENCES books(book_id) ON DELETE CASCADE,
    lesson_number INT       NOT NULL,
    content       TEXT      NOT NULL,
    questions     JSONB,
    -- один и тот же номер не может повторяться в рамках одной книги
    UNIQUE (book_id, lesson_number)
);

CREATE INDEX IF NOT EXISTS idx_lessons_book_id ON lessons (book_id);

-- ── Пользователи: профиль и текущий прогресс ──
-- vk_id — идентификатор пользователя VK (peer_id без минуса);
-- unique: один профиль на одного пользователя VK.
-- progress — JSONB: статус/попытки/последняя оценка по каждому уроку.
-- schedule_settings — под задачу 5 (расписание), пока NULL.
CREATE TABLE IF NOT EXISTS users (
    user_id           BIGSERIAL   PRIMARY KEY,
    vk_id             BIGINT      NOT NULL UNIQUE,
    current_book_id   BIGINT      REFERENCES books(book_id) ON DELETE SET NULL,
    current_lesson    INT,
    progress          JSONB       NOT NULL DEFAULT '{}'::jsonb,
    schedule_settings JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
