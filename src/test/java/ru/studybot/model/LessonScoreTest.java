package ru.studybot.model;

import org.junit.jupiter.api.Test;
import ru.studybot.model.LessonScore.Status;

import static org.junit.jupiter.api.Assertions.*;

class LessonScoreTest {

    // Тестовый урок: 5 вопросов по 1 баллу + 1 краткий на 2 = 7 макс. Возьмём 5 для простоты.
    @Test
    void eightyPercentIsPassed() {
        assertEquals(Status.PASSED, new LessonScore(4, 5).getStatus());
        assertEquals(Status.PASSED, new LessonScore(5, 5).getStatus());
    }

    @Test
    void sixtyToSeventyNineIsConditionallyPassed() {
        assertEquals(Status.CONDITIONALLY_PASSED, new LessonScore(3, 5).getStatus());
    }

    @Test
    void belowSixtyIsFailed() {
        assertEquals(Status.FAILED, new LessonScore(2, 5).getStatus());
        assertEquals(Status.FAILED, new LessonScore(0, 5).getStatus());
    }

    @Test
    void boundaryExactlyEightyIsPassedNotConditionally() {
        assertEquals(Status.PASSED, new LessonScore(80, 100).getStatus());
        assertEquals(Status.CONDITIONALLY_PASSED, new LessonScore(79, 100).getStatus());
    }

    @Test
    void percentIsCalculatedCorrectly() {
        assertEquals(80, new LessonScore(4, 5).percent());
        assertEquals(100, new LessonScore(5, 5).percent());
        assertEquals(0, new LessonScore(0, 5).percent());
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LessonScore(6, 5));
        assertThrows(IllegalArgumentException.class, () -> new LessonScore(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> new LessonScore(0, 0));
    }
}
