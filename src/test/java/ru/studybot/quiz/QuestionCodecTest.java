package ru.studybot.quiz;

import org.junit.jupiter.api.Test;
import ru.studybot.model.Question;
import ru.studybot.model.QuestionType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionCodecTest {

    @Test
    void roundTrip_preservesAllFields() {
        Question original = new Question(1, QuestionType.MULTIPLE_CHOICE,
                "Что такое инкапсуляция?",
                List.of("Сокрытие деталей реализации", "Механизм наследования",
                        "Передача свойств родителя", "Переопределение методов"),
                List.of("Сокрытие деталей реализации"),
                List.of());

        String json = QuestionCodec.toJson(List.of(original));
        List<Question> restored = QuestionCodec.fromJson(json);

        assertEquals(1, restored.size());
        Question q = restored.get(0);
        assertEquals(original.getQuestionNumber(), q.getQuestionNumber());
        assertEquals(original.getType(), q.getType());
        assertEquals(original.getText(), q.getText());
        assertEquals(original.getOptions(), q.getOptions());
        assertEquals(original.getCorrectAnswers(), q.getCorrectAnswers());
        assertEquals(original.getMaxScore(), q.getMaxScore());
        // equals не учитывает id (он генерируется заново), сравниваем отдельно:
        assertEquals(original.getId(), q.getId());
    }

    @Test
    void roundTrip_shortAnswerWithKeywords() {
        Question original = new Question(2, QuestionType.SHORT_ANSWER,
                "Какой принцип ООП связан с сокрытием деталей?",
                List.of(),
                List.of("Инкапсуляция — это сокрытие деталей реализации"),
                List.of("инкапсуляция", "сокрытие", "реализация"));

        String json = QuestionCodec.toJson(List.of(original));
        List<Question> restored = QuestionCodec.fromJson(json);

        assertEquals(original.getKeywords(), restored.get(0).getKeywords());
        assertEquals(original.getCorrectAnswers(), restored.get(0).getCorrectAnswers());
    }

    @Test
    void emptyOrNullJsonReturnsEmptyList() {
        assertTrue(QuestionCodec.fromJson(null).isEmpty());
        assertTrue(QuestionCodec.fromJson("").isEmpty());
        assertTrue(QuestionCodec.fromJson("   ").isEmpty());
    }

    @Test
    void corruptedJsonThrowsWithClearMessage() {
        assertThrows(IllegalStateException.class, () -> QuestionCodec.fromJson("{broken"));
    }
}
