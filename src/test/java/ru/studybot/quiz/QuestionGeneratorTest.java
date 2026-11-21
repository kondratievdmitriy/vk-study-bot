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
    void trueFalseUsesFullSentenceAndNegationForFalse() {
        String block = """
                RLE — это базовый алгоритм сжатия данных.
                Инкапсуляция является фундаментальным принципом объектно-ориентированного программирования.
                """;
        Lesson lesson = new Lesson("b1", 1, "Глава 1", block);
        List<Question> questions = generator.generate(lesson, List.of(lesson));
        List<Question> tf = questions.stream()
                .filter(q -> q.getType() == QuestionType.TRUE_FALSE).toList();

        assertEquals(2, tf.size());
        // Истинный: предложение целиком, без подмены предиката обрывком
        assertTrue(tf.get(0).getText().startsWith("RLE — это базовый алгоритм"));
        assertEquals("Верно", tf.get(0).getCorrectAnswers().get(0));
        // Ложный: отрицание у связки, а не обрывок-дистрактор
        assertTrue(tf.get(1).getText().contains("не является"));
        assertEquals("Неверно", tf.get(1).getCorrectAnswers().get(0));
    }

    @Test
    void duplicatedTermInSubjectIsCollapsed() {
        // Регрессия: термин отдельной строкой (артефакт PDF) склеивался со следующим
        // предложением и дублировался в вопросах: «что такое X X?»
        String block = """
                Jump-кодирование
                Jump-кодирование — это вариация RLE, кодирующая неповторяющиеся последовательности.
                """;
        Lesson lesson = new Lesson("b1", 1, "Глава 1", block);
        List<Question> questions = generator.generate(lesson, List.of(lesson));

        assertFalse(questions.isEmpty());
        Question shortAnswer = questions.stream()
                .filter(q -> q.getType() == QuestionType.SHORT_ANSWER).findFirst().orElseThrow();
        assertTrue(shortAnswer.getText().contains("что такое Jump-кодирование?"),
                "Повторяющийся термин должен быть схлопнут: " + shortAnswer.getText());
        assertFalse(shortAnswer.getText().contains("Jump-кодирование Jump-кодирование"));
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
    @Test
    void definitionsBrokenByPdfLineBreaksAreStillFound() {
        // Регрессия: PDF извлекает текст с жёсткими переносами внутри предложений.
        // Регулярки определений (`.` не матчит \n) не находили ни одного
        // определения, и /quiz отвечал «текст слишком короткий».
        String wrapped = """
                RLE (Run-Length Encoding) — это алгоритм сжатия данных,
                который кодирует повторяющиеся символы.
                Escape-кодирование решает проблему разделения данных
                и управляющей информации.
                """;
        Lesson lesson = new Lesson("b1", 1, "Глава 1", wrapped);
        List<Question> questions = generator.generate(lesson, List.of(lesson));

        assertFalse(questions.isEmpty(), "Переносы строк не должны ломать генерацию вопросов");
        assertTrue(questions.stream().anyMatch(q -> q.getType() == QuestionType.SHORT_ANSWER),
                "Определение, разорванное переносом, должно стать вопросом");
    }

}
