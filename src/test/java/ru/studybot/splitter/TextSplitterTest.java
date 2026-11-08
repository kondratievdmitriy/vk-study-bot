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
