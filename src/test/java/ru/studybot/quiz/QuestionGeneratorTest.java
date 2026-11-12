package ru.studybot.quiz;

import org.junit.jupiter.api.Test;
import ru.studybot.model.Lesson;
import ru.studybot.model.Question;
import ru.studybot.model.QuestionType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionGeneratorTest {
    private final QuestionGenerator generator = new QuestionGenerator();

    private static final String BLOCK = """
            Инкапсуляция — это сокрытие внутренних деталей реализации класса от внешнего мира.
            Инкапсуляция является фундаментальным принципом объектно-ориентированного программирования.
            Наследование — это механизм передачи свойств и методов родительского класса классу-потомку.
            Полиморфизм позволяет использовать единый интерфейс для различных реализаций.
            """;

    @Test
    void generatesQuestionsOfEachType() {
        Lesson lesson = new Lesson("b1", 1, "Глава 1", BLOCK);
        List<Question> questions = generator.generate(lesson, List.of(lesson));

        assertTrue(questions.stream().anyMatch(q -> q.getType() == QuestionType.SHORT_ANSWER));
        assertTrue(questions.stream().anyMatch(q -> q.getType() == QuestionType.MULTIPLE_CHOICE));
        assertTrue(questions.stream().anyMatch(q -> q.getType() == QuestionType.TRUE_FALSE));
        assertTrue(questions.size() >= 2, "Должно быть не менее 2 вопросов");
    }

    @Test
    void choiceHasFourOptionsAndSingleCorrect() {
        Lesson lesson = new Lesson("b1", 1, "Глава 1", BLOCK);
        List<Question> questions = generator.generate(lesson, List.of(lesson));
        Question choice = questions.stream()
                .filter(q -> q.getType() == QuestionType.MULTIPLE_CHOICE)
                .findFirst().orElseThrow();

        assertEquals(4, choice.getOptions().size());
        long correctCount = choice.getOptions().stream()
                .filter(o -> choice.getCorrectAnswers().stream().anyMatch(o::equalsIgnoreCase))
                .count();
        assertEquals(1, correctCount, "Правильный ответ должен встречаться ровно один раз");
    }

    @Test
    void trueFalseAlternatesTrueAndFalse() {
        Lesson lesson = new Lesson("b1", 1, "Глава 1", BLOCK);
        List<Question> questions = generator.generate(lesson, List.of(lesson));
        List<Question> tfQuestions = questions.stream()
                .filter(q -> q.getType() == QuestionType.TRUE_FALSE).toList();
        if (tfQuestions.size() >= 2) {
            assertNotEquals(tfQuestions.get(0).getCorrectAnswers(),
                    tfQuestions.get(1).getCorrectAnswers(),
                    "Вопросы верно/неверно должны чередоваться, чтобы нельзя было отвечать всегда одинаково");
        }
    }

    @Test
    void generatedQuestionsSurviveJsonRoundTrip() {
        Lesson lesson = new Lesson("b1", 1, "Глава 1", BLOCK);
        List<Question> questions = generator.generate(lesson, List.of(lesson));
        assertFalse(questions.isEmpty());

        List<Question> restored = QuestionCodec.fromJson(QuestionCodec.toJson(questions));
        assertEquals(questions, restored);
    }

    @Test
    void emptyOrBlankContentYieldsNoQuestions() {
        Lesson empty = new Lesson("b1", 1, "Глава 1", "   ");
        assertTrue(generator.generate(empty, List.of(empty)).isEmpty());
        assertTrue(generator.generate(null, null).isEmpty());
    }
}
