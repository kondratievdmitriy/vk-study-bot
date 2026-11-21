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
        assertTrue(tf.get(0).getText().startsWith("RLE — это базовый алгоритм"));
        assertEquals("Верно", tf.get(0).getCorrectAnswers().get(0));
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
    void questionsDrawFromDifferentSentences() {
        // Регрессия: раньше все 4 вопроса строились по первому определению блока.
        // Порядок предложений в генераторе зависит от их оценки, поэтому проверяем
        // не конкретный термин, а сам факт: субъекты краткого ответа и выбора
        // варианта должны различаться.
        Lesson lesson = new Lesson("b1", 1, "Глава 1", BLOCK);
        List<Question> questions = generator.generate(lesson, List.of(lesson));

        Question shortQ = questions.stream()
                .filter(q -> q.getType() == QuestionType.SHORT_ANSWER).findFirst().orElseThrow();
        Question choiceQ = questions.stream()
                .filter(q -> q.getType() == QuestionType.MULTIPLE_CHOICE).findFirst().orElseThrow();

        String shortSubject = subjectAfter(shortQ.getText());
        String choiceSubject = subjectAfter(choiceQ.getText());
        assertFalse(shortSubject.isBlank());
        assertFalse(choiceSubject.isBlank());
        assertNotEquals(shortSubject, choiceSubject,
                "Вопросы должны строиться по разным определениям: "
                        + shortQ.getText() + " | " + choiceQ.getText());
    }

    @Test
    void discourseMarkersTrailingPunctuationAndOrNegation() {
        // Регрессия: «Вывод:» попадал в текст вопросов, хвостовая запятая —
        // в субъект, а «не A или B» было грамматически некорректно.
        String block = """
                Вывод: Виртуальная машина, построенная на принципах фон Неймана, — это логическое продолжение классической архитектуры.
                Виртуальная машина (ВМ) — это программная или аппаратная среда, эмулирующая работу физического компьютера.
                """;
        Lesson lesson = new Lesson("b1", 1, "Глава 1", block);
        List<Question> questions = generator.generate(lesson, List.of(lesson));

        assertFalse(questions.isEmpty());
        for (Question q : questions) {
            assertFalse(q.getText().contains("Вывод"),
                    "Служебное вступление должно срезаться: " + q.getText());
        }

        String negated = generator.negate(
                "Виртуальная машина (ВМ) — это программная или аппаратная среда, эмулирующая работу физического компьютера.");
        assertTrue(negated.contains("не программная и не аппаратная"),
                "Перечисление под отрицанием должно разворачиваться в «и не»: " + negated);

        String cleaned = QuestionGenerator.cleanSubject(
                "Вывод: Виртуальная машина, построенная на принципах фон Неймана,");
        assertFalse(cleaned.startsWith("Вывод"), "Вступление должно срезаться: " + cleaned);
        assertFalse(cleaned.endsWith(",") || cleaned.endsWith(";"),
                "Хвостовая пунктуация должна срезаться: " + cleaned);
        assertTrue(cleaned.startsWith("Виртуальная машина"), "Субъект сохраняется: " + cleaned);

        List<Question> tfFalse = questions.stream()
                .filter(q -> q.getType() == QuestionType.TRUE_FALSE
                        && q.getCorrectAnswers().contains("Неверно")).toList();
        for (Question q : tfFalse) {
            assertFalse(q.getText().matches("(?s).*не\\s+[^,.;:!?]+\\s+или\\s+.*"),
                    "Перечисление под отрицанием должно быть развёрнуто: " + q.getText());
        }
    }

    @Test
    void collapseRepeatedRespectsWordBoundaries() {
        // Регрессия: схлопывание повторов без границ слова превращало
        // «фон Неймана» в «фонеймана» (н + Н сворачивались как дубль).
        assertEquals("фон Неймана", QuestionGenerator.cleanSubject("фон Неймана"));
        // Хвостовая пунктуация срезается cleanSubject по его контракту,
        // поэтому вход с запятой даёт строку без неё.
        assertEquals("фон Неймана", QuestionGenerator.cleanSubject("фон Неймана,"));
        assertEquals("Jump-кодирование", QuestionGenerator.cleanSubject("Jump-кодирование Jump-кодирование"));
    }

    @Test
    void colonDefinitionsBecomeQuestions() {
        // Новое поведение: определения через двоеточие («Принцип адресности: …»),
        // типичные для пунктов списков в учебных PDF, распознаются и порождают вопросы.
        String block = """
                Принцип однородности памяти: программы и данные хранятся в одной и той же памяти.
                Принцип адресности: память состоит из пронумерованных ячеек.
                """;
        Lesson lesson = new Lesson("b1", 1, "Глава 1", block);
        List<Question> questions = generator.generate(lesson, List.of(lesson));

        assertFalse(questions.isEmpty());
        assertTrue(questions.stream().anyMatch(q -> q.getType() == QuestionType.SHORT_ANSWER
                        && q.getText().contains("что такое Принцип")),
                "Краткий ответ должен строиться по определению с двоеточием");
        assertTrue(questions.stream().anyMatch(q -> q.getType() == QuestionType.TRUE_FALSE),
                "Определения с двоеточием должны пополнять запас верно/неверно");
    }

    @Test
    void numberedListItemsAreNotDefinitions() {
        // Списочные обрывки «X: 1. …» не должны превращаться в вопросы
        // с мусорным предикатом.
        String block = """
                Пример работы ВМ: 1. Гостевая ОС загружает программу в виртуальную память.
                Виртуальная машина — это программная среда, эмулирующая работу компьютера.
                """;
        Lesson lesson = new Lesson("b1", 1, "Глава 1", block);
        List<Question> questions = generator.generate(lesson, List.of(lesson));

        assertFalse(questions.isEmpty());
        assertTrue(questions.stream().noneMatch(q -> q.getText().contains("1. Гостевая")),
                "Нумерованный список не должен попадать в предикаты вопросов");
    }

    @Test
    void trueFalsePrefersUnusedSentences() {
        // Регрессия: при достатке определений верно/неверно не должно
        // переиспользовать предложения краткого ответа и выбора варианта.
        String block = """
                RLE — это алгоритм сжатия данных без потерь.
                Хаффман — это алгоритм оптимального префиксного кодирования.
                Словарь методов LZ77: повторяющиеся фрагменты заменяются ссылками на ранее встреченные данные.
                Степень сжатия: отношение размера исходных данных к размеру сжатых данных.
                """;
        Lesson lesson = new Lesson("b1", 1, "Глава 1", block);
        List<Question> questions = generator.generate(lesson, List.of(lesson));

        Question shortQ = questions.stream()
                .filter(q -> q.getType() == QuestionType.SHORT_ANSWER).findFirst().orElseThrow();
        Question choiceQ = questions.stream()
                .filter(q -> q.getType() == QuestionType.MULTIPLE_CHOICE).findFirst().orElseThrow();
        String shortSubject = subjectAfter(shortQ.getText());
        String choiceSubject = subjectAfter(choiceQ.getText());

        List<Question> tf = questions.stream()
                .filter(q -> q.getType() == QuestionType.TRUE_FALSE).toList();
        assertEquals(2, tf.size(), "При достатке определений должно быть 2 вопроса верно/неверно");
        for (Question q : tf) {
            String statement = q.getText()
                    .replace(" Верно ли это утверждение?", "")
                    .replace("Неверно, что ", "");
            assertFalse(statement.contains(shortSubject),
                    "Верно/неверно не должно переиспользовать предложение краткого ответа: " + q.getText());
            assertFalse(statement.contains(choiceSubject),
                    "Верно/неверно не должно переиспользовать предложение выбора варианта: " + q.getText());
        }
    }

    @Test
    void normalizeSpacesReplacesNbspAnywhere() {
        // NBSP заменяется обычным пробелом в любом месте строки, а не только по краям.
        assertEquals("a b", QuestionGenerator.normalizeSpaces("a\u00A0b"),
                "Ожидалось «a b», получено: " + toCodePoints(QuestionGenerator.normalizeSpaces("a\u00A0b")));
        assertEquals("фон Неймана", QuestionGenerator.normalizeSpaces("фон\u00A0Неймана"),
                "Ожидалось «фон Неймана», получено: "
                        + toCodePoints(QuestionGenerator.normalizeSpaces("фон\u00A0Неймана")));
    }

    @Test
    void nonBreakingSpacesDoNotSurviveInQuestions() {
        // Регрессия: NBSP (U+00A0) из PDF не должен выживать нигде в вопросах —
        // ни в начале, ни в середине. Сквозной сценарий: урок с NBSP -> вопросы,
        // в которых NBSP нет, а «фон Неймана» — с обычным пробелом.
        // Ожидание строится из той же константы, что и вход, поэтому опечатка
        // или невидимый символ в литерале не могут вызвать ложное расхождение.
        String expectedSubject = "Виртуальная машина, построенная на принципах фон Неймана";
        String content = "Вывод:\u00A0" + expectedSubject.replace("фон Неймана", "фон\u00A0Неймана")
                + ", — это логическое продолжение классической архитектуры.\n"
                + "Виртуальная машина (ВМ) — это программная среда, эмулирующая работу физического компьютера.\n";
        Lesson lesson = new Lesson("b1", 1, "Глава 1", content);
        List<Question> questions = generator.generate(lesson, List.of(lesson));

        assertFalse(questions.isEmpty(), "NBSP не должен ломать генерацию вопросов");
        for (Question q : questions) {
            assertFalse(q.getText().contains("\u00A0"),
                    "NBSP не должен выживать в вопросах: " + q.getText()
                            + " | коды: " + toCodePoints(q.getText()));
            assertFalse(q.getText().contains("Вывод"),
                    "Вступление должно срезаться: " + q.getText());
        }
        Question shortQ = questions.stream()
                .filter(q -> q.getType() == QuestionType.SHORT_ANSWER).findFirst().orElseThrow();
        assertTrue(shortQ.getText().contains("фон Неймана"),
                "Субъект должен содержать «фон Неймана» с обычным пробелом: " + shortQ.getText()
                        + " | коды: " + toCodePoints(shortQ.getText()));
    }

    @Test
    void definitionsBrokenByPdfLineBreaksAreStillFound() {
        // Регрессия: PDF извлекает текст с жёсткими переносами внутри предложений.
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

    /** Извлекает субъект из вопросов вида «(Кратко:) что такое <субъект>?». */
    private static String subjectAfter(String questionText) {
        String lower = questionText.toLowerCase();
        int idx = lower.indexOf("что такое ");
        if (idx == -1) return "";
        String tail = questionText.substring(idx + "что такое ".length());
        return tail.endsWith("?") ? tail.substring(0, tail.length() - 1) : tail;
    }

    /** Печатает каждый символ строки его Unicode-кодом — невидимые символы становятся видимыми. */
    private static String toCodePoints(String s) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < s.length(); i++) {
            sb.append(String.format("U+%04X ", s.codePointAt(i)));
        }
        return sb.append("]").toString();
    }
}
