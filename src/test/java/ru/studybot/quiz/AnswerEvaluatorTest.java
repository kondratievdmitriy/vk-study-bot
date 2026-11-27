package ru.studybot.quiz;

import org.junit.jupiter.api.Test;
import ru.studybot.model.LessonScore;
import ru.studybot.model.Question;
import ru.studybot.model.QuestionResult;
import ru.studybot.model.QuestionType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты оценивания ответов: выбор варианта (точно/неверно/мусорный ввод),
 * краткий ответ по ключевым словам (полное покрытие, частичное, нулевое)
 * и сводная оценка урока по границам шкалы LessonScore.
 */
class AnswerEvaluatorTest {
    private final AnswerEvaluator evaluator = new AnswerEvaluator();

    private Question choiceQuestion() {
        return new Question(1, QuestionType.MULTIPLE_CHOICE, "Что такое X?",
                List.of("Правильный вариант", "Дистрактор один", "Дистрактор два", "Дистрактор три"),
                List.of("Правильный вариант"), List.of());
    }

    private Question shortQuestion() {
        return new Question(2, QuestionType.SHORT_ANSWER, "Что такое Y?",
                List.of(),
                List.of("Сокрытие деталей реализации"),
                List.of("сокрытие", "деталей", "реализации"));
    }

    /** Правильный номер варианта — максимум баллов. */
    @Test
    void choiceCorrectAnswerScoresMax() {
        QuestionResult r = evaluator.evaluate(choiceQuestion(), "1");
        assertTrue(r.isCorrect());
        assertEquals(1, r.getScore());
    }

    /** Неверный номер — ноль, не ошибка. */
    @Test
    void choiceWrongAnswerScoresZero() {
        QuestionResult r = evaluator.evaluate(choiceQuestion(), "2");
        assertFalse(r.isCorrect());
        assertEquals(0, r.getScore());
    }

    /** Мусор и номер вне диапазона — ноль без исключений: пользователь
     *  не должен получать stacktrace за опечатку. */
    @Test
    void choiceInvalidInputIsNotErrorButZero() {
        QuestionResult r = evaluator.evaluate(choiceQuestion(), "abc");
        assertFalse(r.isCorrect());
        assertEquals(0, r.getScore());
        // и для номера за пределами диапазона
        QuestionResult r2 = evaluator.evaluate(choiceQuestion(), "9");
        assertFalse(r2.isCorrect());
    }

    /** Все ключевые слова краткого ответа — максимум (2 балла). */
    @Test
    void shortAnswerWithAllKeywordsScoresMax() {
        QuestionResult r = evaluator.evaluate(shortQuestion(), "Это сокрытие деталей реализации!");
        assertTrue(r.isCorrect());
        assertEquals(2, r.getScore());
    }

    /** 2 из 3 ключевых слов — частичный балл (1 из 3 = 33% → 0). */
    @Test
    void shortAnswerWithHalfKeywordsScoresPartial() {
        // 1 из 3 ключевых слов = 33% -> 0; 2 из 3 = 67% -> 1
        QuestionResult r = evaluator.evaluate(shortQuestion(), "сокрытие деталей");
        assertEquals(1, r.getScore());
    }

    /** Ни одного ключевого слова — ноль. */
    @Test
    void shortAnswerWithNoKeywordsScoresZero() {
        QuestionResult r = evaluator.evaluate(shortQuestion(), "что-то совсем другое");
        assertEquals(0, r.getScore());
    }

    /** Сводная оценка урока: 5/5 и 4/5 — PASSED, 3/5 — CONDITIONALLY_PASSED,
     *  2/5 — FAILED (границы шкалы на живых данных). */
    @Test
    void lessonScoreBoundaries() {
        List<Question> fiveSingleScore = List.of(
                choiceQuestion(), choiceQuestion(), choiceQuestion(), choiceQuestion(), choiceQuestion());
        List<QuestionResult> allCorrect = fiveSingleScore.stream()
                .map(q -> evaluator.evaluate(q, "1")).toList();
        LessonScore perfect = evaluator.evaluateLesson(fiveSingleScore, allCorrect);
        assertEquals(LessonScore.Status.PASSED, perfect.getStatus());

        // 4 из 5 = 80% -> PASSED; 3 из 5 = 60% -> CONDITIONALLY; 2 из 5 -> FAILED
        QuestionResult wrong = evaluator.evaluate(choiceQuestion(), "2");
        assertEquals(LessonScore.Status.PASSED,
                evaluator.evaluateLesson(fiveSingleScore, List.of(
                        allCorrect.get(0), allCorrect.get(1), allCorrect.get(2), allCorrect.get(3), wrong)).getStatus());
        assertEquals(LessonScore.Status.CONDITIONALLY_PASSED,
                evaluator.evaluateLesson(fiveSingleScore, List.of(
                        allCorrect.get(0), allCorrect.get(1), allCorrect.get(2), wrong, wrong)).getStatus());
        assertEquals(LessonScore.Status.FAILED,
                evaluator.evaluateLesson(fiveSingleScore, List.of(
                        allCorrect.get(0), allCorrect.get(1), wrong, wrong, wrong)).getStatus());
    }

    /** Пустой урок — null, а не исключение. */
    @Test
    void emptyLessonReturnsNull() {
        assertNull(evaluator.evaluateLesson(List.of(), List.of()));
    }
}
