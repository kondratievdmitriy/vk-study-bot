package ru.studybot.splitter;

import ru.studybot.model.Lesson;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разделяет текст материала на блоки-уроки.
 * <p>
 * Этап 1. Структурный анализ: поиск заголовков (markdown "#", "Глава N",
 * "Раздел N", "§ N", "1.2 Название"). Если заголовков достаточно — текст
 * делится по ним.
 * </p>
 * <p>
 * Этап 2. Fallback: если заголовков мало, текст делится по объёму —
 * группировка абзацев в блоки по 500–800 слов; слишком длинные абзацы
 * дробятся по предложениям.
 * </p>
 * <p>
 * Этап 3. Нормализация: чистка пробелов, нумерация уроков с 1,
 * перекрытие соседних блоков (конец предыдущего попадает в начало следующего),
 * чтобы не разрывать связность изложения.
 * </p>
 */
public class TextSplitter {

    /** Заголовок: markdown, «Глава/Тема/Раздел/Параграф N ...», «§ N ...», «1.2 Название». */
    private static final Pattern HEADING_LINE = Pattern.compile(
            "^\\s*(#{1,3}\\s+\\S.*"
                    + "|(?:Глава|Тема|Раздел|Параграф)\\b\\s*№?\\s*[\\d.]*[\\s.:—–-]*\\S.*"
                    + "|§\\s*[\\d.]+\\s*\\S.*"
                    + "|\\d+(?:\\.\\d+)+\\s+\\S.*)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                    | Pattern.UNICODE_CHARACTER_CLASS); // <-- добавлен этот флаг


    private static final int MIN_WORDS = 500;
    private static final int MAX_WORDS = 800;
    private static final int OVERLAP_MAX_CHARS = 200;

    /**
     * Разбивает текст на уроки одной книги.
     *
     * @param text   полный текст материала
     * @param bookId идентификатор книги, к которой относятся уроки
     * @return список уроков с нумерацией с 1; пустой список, если текст пустой
     */
    public List<Lesson> split(String text, String bookId) {
        List<Lesson> lessons = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lessons;
        }
        String clean = normalize(text);

        List<Section> sections = splitByHeadings(clean);
        if (sections.size() < 2) {
            sections = splitByWordCount(clean);
        }

        String previousContent = null;
        for (Section section : sections) {
            String content = section.content;
            // Перекрытие делаем только у блоков без заголовков:
            // главы и так завершаются сами по себе.
            if (previousContent != null && !section.titled) {
                String overlap = makeOverlap(previousContent);
                if (!overlap.isEmpty()) {
                    content = overlap + "\n\n" + content;
                }
            }
            int number = lessons.size() + 1;
            String topic = section.title != null ? section.title : "Урок " + number;
            lessons.add(new Lesson(bookId, number, topic, content));
            previousContent = section.content;
        }
        return lessons;
    }

    /** Этап 1: разбиение по строкам-заголовкам. */
    private List<Section> splitByHeadings(String text) {
        List<Section> result = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        StringBuilder currentContent = new StringBuilder();
        String currentTitle = null;
        String pendingIntro = null;

        for (String line : lines) {
            Matcher m = HEADING_LINE.matcher(line);
            if (m.matches()) {
                String content = currentContent.toString().strip();
                if (!content.isEmpty()) {
                    if (currentTitle == null && result.isEmpty()) {
                        // Текст до первого заголовка — это вводка.
                        // Запоминаем и прильнём к первому уроку, а не выделяем в отдельный блок.
                        pendingIntro = content;
                    } else {
                        result.add(new Section(currentTitle, content, currentTitle != null));
                    }
                }
                currentTitle = cleanTitle(m.group(1));
                currentContent = new StringBuilder();
            } else {
                currentContent.append(line).append('\n');
            }
        }
        String content = currentContent.toString().strip();
        if (!content.isEmpty()) {
            if (currentTitle == null && result.isEmpty()) {
                pendingIntro = content;
            } else {
                result.add(new Section(currentTitle, content, currentTitle != null));
            }
        }

        // Вводка приклеивается в начало первого заголовкованного урока
        if (pendingIntro != null && !result.isEmpty()) {
            Section first = result.get(0);
            result.set(0, new Section(first.title, pendingIntro + "\n\n" + first.content, first.titled));
        } else if (pendingIntro != null) {
            // Страховка: заголовков не нашлось вовсе — вводка становится единственным блоком
            result.add(new Section(null, pendingIntro, false));
        }
        return result;
    }


    /** Убирает markdown-решётки из заголовка. */
    private String cleanTitle(String line) {
        return line.strip().replaceFirst("^#{1,3}\\s*", "").strip();
    }

    /** Этап 2: разбиение по объёму (fallback). */
    private List<Section> splitByWordCount(String text) {
        List<String> paragraphs = splitParagraphs(text);
        List<Section> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int words = 0;

        for (String paragraph : paragraphs) {
            int pWords = countWords(paragraph);

            if (pWords > MAX_WORDS) {
                // Сначала сбрасываем накопленное, потом дробим большой абзац по предложениям
                if (words >= MIN_WORDS) {
                    result.add(new Section(null, current.toString().strip(), false));
                    current = new StringBuilder();
                    words = 0;
                }
                for (String chunk : splitBySentences(paragraph)) {
                    result.add(new Section(null, chunk, false));
                }
                continue;
            }

            if (words > 0 && words + pWords > MAX_WORDS) {
                result.add(new Section(null, current.toString().strip(), false));
                current = new StringBuilder();
                words = 0;
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(paragraph.strip());
            words += pWords;
        }

        if (current.length() > 0) {
            String rest = current.toString().strip();
            if (result.isEmpty()) {
                result.add(new Section(null, rest, false));
            } else if (countWords(rest) >= MIN_WORDS / 2) {
                result.add(new Section(null, rest, false));
            } else {
                // Хвост слишком маленький — приклеиваем к последнему блоку
                Section last = result.remove(result.size() - 1);
                result.add(new Section(null, last.content + "\n\n" + rest, false));
            }
        }
        return result;
    }

    /** Абзацы: по пустым строкам, если их нет — по переводам строк. */
    private List<String> splitParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = text.split("\n\\s*\n");
        if (parts.length == 1) {
            for (String line : text.split("\n")) {
                if (!line.isBlank()) paragraphs.add(line);
            }
            return paragraphs;
        }
        for (String p : parts) {
            if (!p.isBlank()) paragraphs.add(p);
        }
        return paragraphs;
    }

    /** Дробление длинного абзаца на куски по предложениям. */
    private List<String> splitBySentences(String paragraph) {
        String[] sentences = paragraph.strip().split("(?<=[.!?…])\\s+");
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int words = 0;
        for (String sentence : sentences) {
            int w = countWords(sentence);
            if (words >= MIN_WORDS && words + w > MAX_WORDS) {
                chunks.add(current.toString().strip());
                current = new StringBuilder();
                words = 0;
            }
            if (current.length() > 0) current.append(' ');
            current.append(sentence.strip());
            words += w;
        }
        if (current.length() > 0) chunks.add(current.toString().strip());
        return chunks;
    }

    /** Перекрытие: 1–2 последних предложения предыдущего блока, не более OVERLAP_MAX_CHARS. */
    private String makeOverlap(String content) {
        String[] sentences = content.strip().split("(?<=[.!?…])\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, sentences.length - 2); i < sentences.length; i++) {
            if (sb.length() + sentences[i].length() > OVERLAP_MAX_CHARS) break;
            if (sb.length() > 0) sb.append(' ');
            sb.append(sentences[i]);
        }
        return sb.toString();
    }

    private String normalize(String text) {
        return text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" ?\n ?", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .strip();
    }

    private int countWords(String s) {
        String trimmed = s.strip();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    /** Внутреннее представление блока: заголовок (может отсутствовать) + текст. */
    private static class Section {
        final String title;
        final String content;
        final boolean titled;

        Section(String title, String content, boolean titled) {
            this.title = title;
            this.content = content;
            this.titled = titled;
        }
    }
}
