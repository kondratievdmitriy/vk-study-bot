package ru.studybot.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.studybot.model.*;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.repository.UserProgressRepository;
import ru.studybot.repository.impl.InMemoryUserProgressRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты /next: переход между уроками зависит от результата последнего
 * теста. Состояния прогресса задаются вручную, чтобы проверить каждую ветку
 * автомата (STUDYING → PASSED/FAILED → REPEATING) изолированно.
 */
class NextCommandTest {

    private NextCommand nextCommand;
    private BookRepository bookRepo;
    private LessonRepository lessonRepo;
    private UserProgressRepository progressRepo;
    private long peerId = 200L;

    @BeforeEach
    void setUp() {
        bookRepo = new MockBookRepository();
        lessonRepo = new MockLessonRepository();
        progressRepo = new InMemoryUserProgressRepository();

        // Предварительно создаем прогресс, как будто пользователь уже учится
        UserProgress initialProgress = new UserProgress(peerId);
        initialProgress.setBookId("book_1");
        initialProgress.setCurrentLessonNumber(1);
        initialProgress.setStatus(UserProgress.Status.STUDYING);
        progressRepo.save(initialProgress);

        nextCommand = new NextCommand(bookRepo, lessonRepo, progressRepo);
    }

    /** Тест ещё не сдан (lastLessonStatus не задан) — переход заблокирован. */
    @Test
    void nextWhenTestNotTaken_blocked() {
        MessageContext context = new MessageContext(peerId, "", List.of());
        String result = nextCommand.execute(context);

        assertTrue(result.contains("Вы ещё не прошли тест"));
    }

    /** После FAILED переход заблокирован, статус уходит в REPEATING,
     *  номер урока не растёт. */
    @Test
    void nextWhenFailed_blockedAndRepeating() {
        // Симулируем, что напарник записал статус FAILED
        UserProgress progress = progressRepo.findByPeerId(peerId);
        progress.setLastLessonStatus(LessonScore.Status.FAILED);
        progressRepo.save(progress);

        MessageContext context = new MessageContext(peerId, "", List.of());
        String result = nextCommand.execute(context);

        assertTrue(result.contains("Тест не сдан"));
        assertTrue(result.contains("/learn"));

        // Проверяем, что номер урока НЕ увеличился
        assertEquals(1, progressRepo.findByPeerId(peerId).getCurrentLessonNumber());
        assertEquals(UserProgress.Status.REPEATING, progressRepo.findByPeerId(peerId).getStatus());
    }

    /** После PASSED переход разрешён: номер урока растёт, статус возвращается
     *  в STUDYING. */
    @Test
    void nextWhenPassed_successTransition() {
        // Симулируем успешную сдачу теста
        UserProgress progress = progressRepo.findByPeerId(peerId);
        progress.setLastLessonStatus(LessonScore.Status.PASSED);
        progressRepo.save(progress);

        MessageContext context = new MessageContext(peerId, "", List.of());
        String result = nextCommand.execute(context);

        assertTrue(result.contains("Переход к следующему уроку"));
        assertTrue(result.contains("Урок 2"));

        // Проверяем, что номер урока увеличился
        assertEquals(2, progressRepo.findByPeerId(peerId).getCurrentLessonNumber());
        assertEquals(UserProgress.Status.STUDYING, progressRepo.findByPeerId(peerId).getStatus());
    }

    // --- Вспомогательные моки (те же, что и в LearnCommandTest) ---
    private static class MockBookRepository implements BookRepository {
        @Override public void save(Book book) {}
        @Override public List<Book> findAll() { return List.of(new Book("Java")); }
        @Override public Book findByTitle(String title) { return "Java".equals(title) ? new Book("Java") : null; }
    }

    private static class MockLessonRepository implements LessonRepository {
        @Override public void save(Lesson lesson) {}
        @Override public List<Lesson> findAll() { return List.of(); }
        @Override public List<Lesson> findByBookId(String bookId) {
            Lesson l1 = new Lesson("book_1", 1, "Введение в Java", "Текст первого урока.");
            Lesson l2 = new Lesson("book_1", 2, "Переменные", "Текст второго урока.");
            return List.of(l1, l2);
        }
        @Override public void deleteByBookId(String bookId) {
            // заглушка: в этих тестах удаление уроков не используется
        }
    }
}
