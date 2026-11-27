package ru.studybot.model;

import org.junit.jupiter.api.Test;
import ru.studybot.model.LessonScore.Status;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты шкалы оценивания LessonScore: PASSED с 80%, CONDITIONALLY_PASSED
 * в диапазоне [60%, 80%), FAILED ниже. Проверяются и проценты, и валидация
 * аргументов конструктора. Оценка зависит только от пары (score, maxScore) —
 * тип вопроса не важен, поэтому фиксированные числа корректны.
 */
class LessonScoreTest {

    /** 80% и выше — PASSED: на шкале из 5 баллов это 4 и 5. */
    @Test
    void eightyPercentIsPassed() {
        assertEquals(Status.PASSED, new LessonScore(4, 5).getStatus());
        assertEquals(Status.PASSED, new LessonScore(5, 5).getStatus());
    }

    /** 60–79% — CONDITIONALLY_PASSED: на шкале из 5 баллов это 3. */
    @Test
    void sixtyToSeventyNineIsConditionallyPassed() {
        assertEquals(Status.CONDITIONALLY_PASSED, new LessonScore(3, 5).getStatus());
    }

    /** Ниже 60% — FAILED: 2 и 0 из 5. */
    @Test
    void belowSixtyIsFailed() {
        assertEquals(Status.FAILED, new LessonScore(2, 5).getStatus());
        assertEquals(Status.FAILED, new LessonScore(0, 5).getStatus());
    }

    /** Граница ровно 80% — уже PASSED, а не CONDITIONALLY_PASSED. */
    @Test
    void boundaryExactlyEightyIsPassedNotConditionally() {
        assertEquals(Status.PASSED, new LessonScore(80, 100).getStatus());
        assertEquals(Status.CONDITIONALLY_PASSED, new LessonScore(79, 100).getStatus());
    }

    /** percent() возвращает целые проценты (score/maxScore * 100). */
    @Test
    void percentIsCalculatedCorrectly() {
        assertEquals(80, new LessonScore(4, 5).percent());
        assertEquals(100, new LessonScore(5, 5).percent());
        assertEquals(0, new LessonScore(0, 5).percent());
    }

    /** Некорректные аргументы (score > max, отрицательные, 0/0) отвергаются. */
    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LessonScore(6, 5));
        assertThrows(IllegalArgumentException.class, () -> new LessonScore(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> new LessonScore(0, 0));
    }
}
