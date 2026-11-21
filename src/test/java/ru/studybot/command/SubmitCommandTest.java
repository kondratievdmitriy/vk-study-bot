package ru.studybot.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.studybot.model.Book;
import ru.studybot.model.Lesson;
import ru.studybot.model.LessonScore;
import ru.studybot.model.Question;
import ru.studybot.model.QuestionType;
import ru.studybot.model.UserProgress;
import ru.studybot.quiz.QuestionCodec;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.repository.UserProgressRepository;
import ru.studybot.repository.impl.InMemoryBookRepository;
import ru.studybot.repository.impl.InMemoryLessonRepository;
import ru.studybot.repository.impl.InMemoryUserProgressRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест стыка /submit → /next: результат теста должен
 * попадать в прогресс пользователя и открывать (или блокировать) переход.
 */
class SubmitCommandTest {

    private static final long PEER_ID = 300L;
    private static final String TITLE = "Тестовая книга";

    private BookRepository bookRepo;
    private LessonRepository lessonRepo;
    private UserProgressRepository progressRepo;
    private SubmitCommand submitCommand;
    private NextCommand nextCommand;

    @BeforeEach
    void setUp() {
        bookRepo = new InMemoryBookRepository();
        lessonRepo = new InMemoryLessonRepository();
        progressRepo = new InMemoryUserProgressRepository();

        Book book = new Book(TITLE);
        bookRepo.save(book);

        // Тест из двух вопросов: выбор варианта (1 б.) и верно/неверно (1 б.)
        Question q1 = new Question(1, QuestionType.MULTIPLE_CHOICE, "Что такое X?",
                List.of("Правильный ответ", "Дистрактор 1", "Дистрактор 2", "Дистрактор 3"),
                List.of("Правильный ответ"), List.of());
        Question q2 = new Question(2, QuestionType.TRUE_FALSE, "X — это Y. Верно?",
                List.of("Верно", "Неверно"),
                List.of("Верно"), List.of());

        Lesson lesson = new Lesson(book.getId(), 1, "Урок 1", "Содержимое урока.");
        lesson.setQuestions(QuestionCodec.toJson(List.of(q1, q2)));
        lessonRepo.save(lesson);

        submitCommand = new SubmitCommand(bookRepo, lessonRepo, progressRepo);
        nextCommand = new NextCommand(bookRepo, lessonRepo, progressRepo);
    }

    @Test
    void correctAnswers_updateProgressAndOpenNext() {
        String result = submitCommand.execute(
                new MessageContext(PEER_ID, TITLE + " 1 | 1; 1", List.of()));

        assertTrue(result.contains("засчитан"));

        UserProgress progress = progressRepo.findByPeerId(PEER_ID);
        assertNotNull(progress, "После теста прогресс должен существовать");
        assertEquals(LessonScore.Status.PASSED, progress.getLastLessonStatus());
        assertEquals(1, progress.getAttemptsForLesson(1));

        String next = nextCommand.execute(new MessageContext(PEER_ID, "", List.of()));
        assertTrue(next.contains("Переход к следующему уроку"));
    }

    @Test
    void failedAnswers_blockNext() {
        submitCommand.execute(new MessageContext(PEER_ID, TITLE + " 1 | 2; 2", List.of()));

        UserProgress progress = progressRepo.findByPeerId(PEER_ID);
        assertEquals(LessonScore.Status.FAILED, progress.getLastLessonStatus());

        String next = nextCommand.execute(new MessageContext(PEER_ID, "", List.of()));
        assertTrue(next.contains("Тест не сдан"));
    }

    @Test
    void conditionallyPassed_stillOpensNext() {
        // 1 из 2 = 50% → FAILED; проверим границу 60%: короткий ответ отсутствует,
        // поэтому возьмём тест с другим числом вопросов: 3 вопроса по 1 б., 2 верных = 67%
        Question q3 = new Question(3, QuestionType.TRUE_FALSE, "Z — это W. Верно?",
                List.of("Верно", "Неверно"), List.of("Верно"), List.of());
        Lesson lesson = lessonRepo.findByBookId(
                bookRepo.findByTitle(TITLE).getId()).get(0);
        lesson.setQuestions(QuestionCodec.toJson(List.of(
                new Question(1, QuestionType.MULTIPLE_CHOICE, "A?", List.of("A1", "A2", "A3", "A4"),
                        List.of("A1"), List.of()),
                new Question(2, QuestionType.MULTIPLE_CHOICE, "B?", List.of("B1", "B2", "B3", "B4"),
                        List.of("B1"), List.of()),
                q3)));
        lessonRepo.save(lesson);

        submitCommand.execute(new MessageContext(PEER_ID, TITLE + " 1 | 1; 1; 2", List.of()));

        assertEquals(LessonScore.Status.CONDITIONALLY_PASSED,
                progressRepo.findByPeerId(PEER_ID).getLastLessonStatus());
        String next = nextCommand.execute(new MessageContext(PEER_ID, "", List.of()));
        assertTrue(next.contains("Переход к следующему уроку"));
    }

    @Test
    void answersCountMismatch_returnsError() {
        String result = submitCommand.execute(
                new MessageContext(PEER_ID, TITLE + " 1 | 1", List.of()));
        assertTrue(result.contains("Ответов"));
        assertNull(progressRepo.findByPeerId(PEER_ID),
                "При ошибке формата прогресс не должен меняться");
    }
}
