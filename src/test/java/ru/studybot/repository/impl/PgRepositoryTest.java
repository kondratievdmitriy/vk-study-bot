package ru.studybot.repository.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.studybot.db.Database;
import ru.studybot.model.Book;
import ru.studybot.model.Lesson;
import ru.studybot.model.LessonScore;
import ru.studybot.model.UserProgress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Интеграционные тесты PostgreSQL-репозиториев (задача 4).
 * <p>
 * Требуют запущенной базы из db.url в config.properties. Если база
 * недоступна — тесты пропускаются, не падают: сборка не должна ломаться
 * на машине без PostgreSQL, а существующие тесты от БД не зависят.
 * </p>
 */
class PgRepositoryTest {

    private static Connection connection;

    @BeforeAll
    static void connect() {
        try {
            connection = Database.getConnection();
        } catch (Exception e) {
            connection = null; // база недоступна — тесты пропустятся
        }
        assumeTrue(connection != null,
                "PostgreSQL недоступен — интеграционные тесты репозиториев пропущены");
    }

    @Test
    void bookSaveFindAndUpdate() {
        PgBookRepository repo = new PgBookRepository();
        String title = "Тестовая книга " + UUID.randomUUID();

        Book book = new Book(title);
        book.setAuthor("Автор");
        book.setTotalLessons(11);
        repo.save(book);

        Book found = repo.findByTitle(title);
        assertNotNull(found);
        assertEquals("Автор", found.getAuthor());
        assertEquals(11, found.getTotalLessons());
        assertEquals(book.getId(), found.getId(), "id должен восстанавливаться из базы");

        // Повторный save — upsert: та же книга, обновлённые поля
        book.setTotalLessons(12);
        repo.save(book);
        assertEquals(12, repo.findByTitle(title).getTotalLessons());

        // Дублей не появляется
        assertEquals(1, repo.findAll().stream().filter(b -> title.equals(b.getTitle())).count());
    }

    @Test
    void lessonSaveUpsertAndFindByBookId() {
        PgBookRepository bookRepo = new PgBookRepository();
        PgLessonRepository lessonRepo = new PgLessonRepository();

        Book book = new Book("Книга для уроков " + UUID.randomUUID());
        bookRepo.save(book);

        Lesson lesson = new Lesson(book.getId(), 1, "1. Тестовый раздел.", "RLE — это алгоритм сжатия.");
        lesson.setQuestions("[{\"type\":\"SHORT_ANSWER\"}]");
        lessonRepo.save(lesson);

        // Upsert: тот же (book_id, lesson_number) — обновление, а не дубль
        lesson.setQuestions("[{\"type\":\"TRUE_FALSE\"}]");
        lessonRepo.save(lesson);

        List<Lesson> found = lessonRepo.findByBookId(book.getId());
        assertEquals(1, found.size(), "upsert не должен плодить дубли уроков");
        assertEquals("1. Тестовый раздел.", found.get(0).getTopic());
        assertTrue(found.get(0).getQuestions().contains("TRUE_FALSE"));

        // Сортировка по номеру урока
        lessonRepo.save(new Lesson(book.getId(), 2, "2. Второй.", "Хаффман — это кодирование."));
        List<Lesson> sorted = lessonRepo.findByBookId(book.getId());
        assertEquals(2, sorted.size());
        assertEquals(1, sorted.get(0).getLessonNumber());
        assertEquals(2, sorted.get(1).getLessonNumber());
    }

    @Test
    void duplicateLessonNumberInSameBookRejectedByDatabase() {
        PgBookRepository bookRepo = new PgBookRepository();
        PgLessonRepository lessonRepo = new PgLessonRepository();

        Book book = new Book("Книга для конфликта " + UUID.randomUUID());
        bookRepo.save(book);
        lessonRepo.save(new Lesson(book.getId(), 1, "1. Первый.", "Текст"));

        // Прямой INSERT с другим id, но тем же (book_id, lesson_number):
        // уникальное ограничение БД должно отклонить его
        assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO lessons (lesson_id, book_id, lesson_number, content) VALUES (?, ?, 1, 'дубль')")) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, book.getId());
                ps.executeUpdate();
            }
        });
    }

    @Test
    void userProgressRoundTrip() {
        PgBookRepository bookRepo = new PgBookRepository();
        PgUserProgressRepository repo = new PgUserProgressRepository();
        long peerId = 900_000_000L + new java.util.Random().nextInt(1_000_000);

        assertNull(repo.findByPeerId(peerId), "нового пользователя быть не должно");

        // Прогресс ссылается на реально существующую книгу:
        // users.current_book_id — внешний ключ на books, и база
        // корректно отклоняет «висячие» ссылки (проверено падением
        // предыдущей версии этого теста).
        Book book = new Book("Книга для прогресса " + UUID.randomUUID());
        bookRepo.save(book);

        UserProgress progress = new UserProgress(peerId);
        progress.setBookId(book.getId());
        progress.setCurrentLessonNumber(2);
        progress.setStatus(UserProgress.Status.STUDYING);
        progress.setLastLessonStatus(LessonScore.Status.CONDITIONALLY_PASSED);
        progress.incrementAttempts();
        progress.incrementAttempts();
        progress.getAttemptsPerLesson().put(1, 3);
        repo.save(progress);

        UserProgress restored = repo.findByPeerId(peerId);
        assertNotNull(restored);
        assertEquals(progress.getBookId(), restored.getBookId());
        assertEquals(2, restored.getCurrentLessonNumber());
        assertEquals(UserProgress.Status.STUDYING, restored.getStatus());
        assertEquals(LessonScore.Status.CONDITIONALLY_PASSED, restored.getLastLessonStatus());
        assertEquals(2, restored.getAttemptsForLesson(2));
        assertEquals(3, restored.getAttemptsForLesson(1));

        // save того же пользователя — обновление, а не дубль
        progress.setLastLessonStatus(LessonScore.Status.PASSED);
        repo.save(progress);
        assertEquals(1, repo.findAll().stream().filter(p -> p.getPeerId() == peerId).count());
        assertEquals(LessonScore.Status.PASSED, repo.findByPeerId(peerId).getLastLessonStatus());
    }
}
