package ru.studybot.repository.impl;

import ru.studybot.db.Database;
import ru.studybot.model.Lesson;
import ru.studybot.repository.LessonRepository;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL-реализация хранилища уроков (задача 4).
 * <p>
 * save() — upsert по уникальному ограничению (book_id, lesson_number):
 * повторная загрузка книги обновляет уроки, включая перезапись
 * сгенерированных вопросов. deleteByBookId() поддерживает сценарий
 * «перезагрузка материала через /upload».
 * </p>
 */
public class PgLessonRepository implements LessonRepository {

    private static final String SAVE_SQL = """
            INSERT INTO lessons (lesson_id, book_id, lesson_number, topic, content, questions)
            VALUES (?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (book_id, lesson_number) DO UPDATE
                SET lesson_id = EXCLUDED.lesson_id,
                    topic = EXCLUDED.topic,
                    content = EXCLUDED.content,
                    questions = EXCLUDED.questions
            """;

    private static final String FIND_ALL_SQL = """
            SELECT lesson_id, book_id, lesson_number, topic, content, questions
            FROM lessons
            ORDER BY book_id, lesson_number
            """;

    private static final String FIND_BY_BOOK_SQL = """
            SELECT lesson_id, book_id, lesson_number, topic, content, questions
            FROM lessons
            WHERE book_id = ?
            ORDER BY lesson_number
            """;

    private static final String DELETE_BY_BOOK_SQL = "DELETE FROM lessons WHERE book_id = ?";

    @Override
    public void save(Lesson lesson) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SAVE_SQL)) {
            ps.setString(1, lesson.getId());
            ps.setString(2, lesson.getBookId());
            ps.setInt(3, lesson.getLessonNumber());
            ps.setString(4, lesson.getTopic());
            ps.setString(5, lesson.getContent());
            if (lesson.getQuestions() == null) {
                ps.setNull(6, java.sql.Types.OTHER); // JSONB NULL
            } else {
                ps.setString(6, lesson.getQuestions());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить урок в PostgreSQL", e);
        }
    }

    @Override
    public List<Lesson> findAll() {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            List<Lesson> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать уроки из PostgreSQL", e);
        }
    }

    @Override
    public List<Lesson> findByBookId(String bookId) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_BOOK_SQL)) {
            ps.setString(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Lesson> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось найти уроки книги в PostgreSQL", e);
        }
    }

    @Override
    public void deleteByBookId(String bookId) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(DELETE_BY_BOOK_SQL)) {
            ps.setString(1, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось удалить уроки книги из PostgreSQL", e);
        }
    }

    private Lesson mapRow(ResultSet rs) throws SQLException {
        Lesson lesson = new Lesson(rs.getString("book_id"), rs.getInt("lesson_number"),
                rs.getString("topic"), rs.getString("content"));
        setIdQuietly(lesson, rs.getString("lesson_id"));
        lesson.setQuestions(rs.getString("questions")); // JSONB читается как текст JSON
        return lesson;
    }

    private static void setIdQuietly(Lesson lesson, String id) {
        try {
            Field field = Lesson.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(lesson, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось восстановить id урока", e);
        }
    }
}
