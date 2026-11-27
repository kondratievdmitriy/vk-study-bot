package ru.studybot.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.studybot.model.Book;
import ru.studybot.model.Lesson;
import ru.studybot.model.UserProgress;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.repository.UserProgressRepository;
import ru.studybot.repository.impl.InMemoryUserProgressRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты /learn: начало изучения книги и продолжение.
 * BookRepository и LessonRepository — минимальные стабы (одна книга «Java»
 * с двумя уроками), прогресс — реальный InMemoryUserProgressRepository:
 * именно его контракт сохранения прогресса и проверяется.
 */
class LearnCommandTest {

    private LearnCommand learnCommand;
    private BookRepository bookRepo;
    private LessonRepository lessonRepo;
    private UserProgressRepository progressRepo;

    @BeforeEach
    void setUp() {
        // Настраиваем моки
        bookRepo = new MockBookRepository();
        lessonRepo = new MockLessonRepository();
        progressRepo = new InMemoryUserProgressRepository(); // Используем реальную in-memory реализацию

        learnCommand = new LearnCommand(bookRepo, lessonRepo, progressRepo);
    }

    /** Старт новой книги: бот показывает первый урок и подсказывает /quiz,
     *  а прогресс создаётся с уроком 1 и статусом STUDYING. */
    @Test
    void startNewBook_success() {
        long peerId = 100L;
        MessageContext context = new MessageContext(peerId, "Java", List.of());

        String result = learnCommand.execute(context);

        assertTrue(result.contains("Урок 1"));
        assertTrue(result.contains("Введение в Java"));
        assertTrue(result.contains("/quiz"));

        // Проверяем, что прогресс сохранился
        UserProgress progress = progressRepo.findByPeerId(peerId);
        assertNotNull(progress);
        assertEquals(1, progress.getCurrentLessonNumber());
        assertEquals(UserProgress.Status.STUDYING, progress.getStatus());
    }

    /** Книги нет в репозитории — пользователь получает внятный отказ, а не исключение. */
    @Test
    void startNewBook_bookNotFound() {
        long peerId = 101L;
        MessageContext context = new MessageContext(peerId, "НесуществующаяКнига", List.of());

        String result = learnCommand.execute(context);
        assertTrue(result.contains("не найдена"));
    }

    /** «/learn» без аргументов при отсутствии прогресса подсказывает, как начать. */
    @Test
    void continueLearning_noBookSelected() {
        long peerId = 102L;
        // Аргументы пустые
        MessageContext context = new MessageContext(peerId, "", List.of());

        String result = learnCommand.execute(context);
        assertTrue(result.contains("Вы ещё не начали изучение"));
    }

    // --- Вспомогательные моки ---

    private static class MockBookRepository implements BookRepository {
        @Override public void save(Book book) {}
        @Override public List<Book> findAll() { return List.of(new Book("Java")); }
        @Override public Book findByTitle(String title) {
            if ("Java".equals(title)) return new Book("Java");
            return null;
        }
    }

    private static class MockLessonRepository implements LessonRepository {
        @Override public void save(Lesson lesson) {}
        @Override public List<Lesson> findAll() { return List.of(); }
        @Override public List<Lesson> findByBookId(String bookId) {
            // Возвращаем два урока для тестов
            Lesson l1 = new Lesson("book_1", 1, "Введение в Java", "Текст первого урока.");
            Lesson l2 = new Lesson("book_1", 2, "Переменные", "Текст второго урока.");
            return List.of(l1, l2);
        }
        @Override public void deleteByBookId(String bookId) {
            // заглушка: в этих тестах удаление уроков не используется
        }
    }
}
