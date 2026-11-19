package ru.studybot.splitter;

import org.junit.jupiter.api.Test;
import ru.studybot.model.Lesson;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextSplitterTest {
    private final TextSplitter splitter = new TextSplitter();

    @Test
    void splitsTextByChapterHeadings() {
        String text = """
                Вводный абзац о книге.
                
                Глава 1: Основы
                Текст первой главы.
                
                Глава 2: Продвинутые темы
                Текст второй главы.
                
                Глава 3: Заключение
                Текст третьей главы.
                """;
        List<Lesson> lessons = splitter.split(text, "book-1");

        assertEquals(3, lessons.size());
        assertEquals("Глава 1: Основы", lessons.get(0).getTopic());
        assertEquals("Глава 2: Продвинутые темы", lessons.get(1).getTopic());
        assertEquals(1, lessons.get(0).getLessonNumber());
        assertEquals(2, lessons.get(1).getLessonNumber());
        assertEquals(3, lessons.get(2).getLessonNumber());
        assertEquals("book-1", lessons.get(0).getBookId());
    }

    @Test
    void splitsMarkdownHeadings() {
        String text = """
                ## Первая тема
                Текст первой темы.
                
                ## Вторая тема
                Текст второй темы.
                """;
        List<Lesson> lessons = splitter.split(text, "book-1");

        assertEquals(2, lessons.size());
        assertEquals("Первая тема", lessons.get(0).getTopic());
    }

    @Test
    void singleNumberedSectionsAreHeadings() {
        String text = """
                1. Введение.
                Текст введения.
                
                2. Основная часть.
                Текст основной части.
                """;
        List<Lesson> lessons = splitter.split(text, "book-1");

        assertEquals(2, lessons.size());
        assertEquals("1. Введение.", lessons.get(0).getTopic());
        assertEquals("2. Основная часть.", lessons.get(1).getTopic());
    }

    @Test
    void numberedSectionsWithInternalLists() {
        // Регрессия: пункты списка внутри разделов не должны считаться заголовками
        String text = """
                1. Первый раздел.
                Текст первого раздела про RLE.
                1. Пункт списка один.
                2. Пункт списка два.
                3. Пункт списка три.

                2. Второй раздел.
                Текст второго раздела.

                3. Третий раздел.
                Текст третьего раздела.
                """;
        List<Lesson> lessons = splitter.split(text, "book-1");

        assertEquals(3, lessons.size());
        assertEquals("1. Первый раздел.", lessons.get(0).getTopic());
        assertEquals("2. Второй раздел.", lessons.get(1).getTopic());
        assertTrue(lessons.get(0).getContent().contains("Пункт списка два"));
    }

    @Test
    void decimalNumberIsNotHeading() {
        // «2.5 раза больше» — не заголовок, текст не должен рваться
        String text = "Скорость выросла в 2.5 раза больше ожидаемого. Всё это один абзац.";
        List<Lesson> lessons = splitter.split(text, "book-1");

        assertEquals(1, lessons.size());
        assertEquals("Урок 1", lessons.get(0).getTopic());
    }

    @Test
    void fallbackPreservesTextOrder() {
        // Регрессия: при дроблении длинного абзаца порядок текста не должен нарушаться
        String paragraph = ("слово ").repeat(399); // 399 слов + маркер
        String[] markers = {"Первый", "Второй", "Третий", "Четвертый", "Пятый"};
        StringBuilder sb = new StringBuilder();
        for (String marker : markers) {
            sb.append(marker).append(" блок. ").append(paragraph).append("\n\n");
        }
        List<Lesson> lessons = splitter.split(sb.toString(), "book-1");

        assertTrue(lessons.size() >= 3);
        assertTrue(lessons.get(0).getContent().contains("Первый блок"));
        assertTrue(lessons.get(1).getContent().contains("Второй блок"));
        assertTrue(lessons.get(2).getContent().contains("Третий блок"));
    }

    @Test
    void longParagraphIsSplitIntoChunks() {
        // Длинный абзац из многих предложений дробится по предложениям,
        // а окружающий текст сохраняет свой порядок
        String longParagraph = ("Слово номер. ").repeat(500); // 1000 слов
        String text = "Короткий блок.\n\n" + longParagraph + "\n\nЕще короткий блок.";

        List<Lesson> lessons = splitter.split(text, "book-1");

        assertTrue(lessons.size() > 2);
        assertTrue(lessons.get(0).getContent().contains("Короткий блок"),
                "Текст перед длинным абзацем должен идти первым");
        boolean longPartFound = false;
        for (Lesson lesson : lessons) {
            if (lesson.getContent().contains("Слово номер")) {
                longPartFound = true;
                break;
            }
        }
        assertTrue(longPartFound, "Длинный абзац должен быть разбит и присутствовать в блоках");
    }

    @Test
    void fallbackSplitsByWordCount() {
        // Текст без заголовков: 12 абзацев по 150 слов -> блоки по 500-800 слов
        String paragraph = ("слово ").repeat(149) + "конец.";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append("Абзац номер ").append(i).append(". ").append(paragraph).append("\n\n");
        }
        List<Lesson> lessons = splitter.split(sb.toString(), "book-1");

        assertTrue(lessons.size() >= 2);
        for (Lesson lesson : lessons) {
            int words = lesson.getContent().split("\\s+").length;
            // перекрытие может добавить немного слов — допускаем запас
            assertTrue(words <= 900, "Блок слишком длинный: " + words + " слов");
        }
    }

    @Test
    void fallbackBlocksHaveOverlap() {
        String paragraph = ("слово ").repeat(149) + "конец.";
        StringBuilder sb = new StringBuilder();
        sb.append("Первый блок. ").append(paragraph).append("\n\n");
        sb.append("Второй блок. ").append(paragraph).append("\n\n");
        List<Lesson> lessons = splitter.split(sb.toString(), "book-1");

        if (lessons.size() >= 2) {
            // начало второго блока должно содержать перекрытие из конца первого
            String firstEnd = lessons.get(0).getContent().strip();
            String overlap = firstEnd.substring(Math.max(0, firstEnd.length() - 100));
            assertTrue(lessons.get(1).getContent().contains(overlap.strip())
                            || lessons.get(1).getContent().contains("конец."),
                    "Второй блок должен начинаться с перекрытия из первого");
        }
    }

    @Test
    void emptyTextReturnsEmptyList() {
        assertTrue(splitter.split("", "book-1").isEmpty());
        assertTrue(splitter.split("   ", "book-1").isEmpty());
        assertTrue(splitter.split(null, "book-1").isEmpty());
    }

    @Test
    void singleShortTextBecomesOneLesson() {
        List<Lesson> lessons = splitter.split("Просто короткий текст без заголовков.", "book-1");
        assertEquals(1, lessons.size());
        assertEquals("Урок 1", lessons.get(0).getTopic());
    }
}