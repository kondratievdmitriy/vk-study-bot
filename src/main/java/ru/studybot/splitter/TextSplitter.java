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
 * "Раздел N", "§ N", "1. Название", "1.2 Название"). Нумерованные разделы
 * распознаются по монотонной нумерации (1, 2, 3, ...), что отличает их от
 * пунктов списков внутри разделов, которые начинаются заново с 1.
 * Заголовки, разорванные переносом строки при извлечении из PDF,
 * склеиваются обратно.
 * </p>
 * <p>
 * Этап 2. Fallback: если заголовков нет вовсе, текст делится по объёму —
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

    /**
     * Заголовок: markdown, «Глава/Тема/Раздел/Параграф/Пункт N ...», «§ N ...»,
     * нумерованные разделы («1. Название», «1.2 Название», «1) Название»).
     * Номер нумерованного раздела захватывается в группу 2.
     * (?-i:[А-ЯЁA-Z]) — после номера обязана идти заглавная буква
     * (без учёта общего флага CASE_INSENSITIVE): отсекает строки
     * вроде «2.5 раза больше».
     */
    private static final Pattern HEADING_LINE = Pattern.compile(
            "^\\s*(#{1,3}\\s+\\S.*"
                    + "|(?:Глава|Тема|Раздел|Параграф|Пункт)\\b\\s*№?\\s*[\\d.]*[\\s.:—–-]*\\S.*"
                    + "|§\\s*[\\d.]+\\s*\\S.*"
                    + "|(\\d+(?:\\.\\d+)*)[.)]?\\s+(?-i:[А-ЯЁA-Z]).*)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                    | Pattern.UNICODE_CHARACTER_CLASS);

    /** Строка, начинающаяся с номера, — для распознавания пунктов списков. */
    private static final Pattern NUMBERED_ITEM = Pattern.compile(
            "\\d+(?:\\.\\d+)*[.)]?\\s+\\S.*");

    /** Заголовок не бывает длиннее — ограждение от ложных срабатываний. */
    private static final int MAX_HEADING_LENGTH = 120;

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
        // Fallback по объёму применяем только если заголовков нет вовсе.
        // Материал из одного раздела («Глава 2: ...») — валидный материал:
        // при size() < 2 его заголовок терялся, и урок получал тему «Урок 1».
        boolean hasTitles = false;
        for (Section s : sections) {
            if (s.titled) {
                hasTitles = true;
                break;
            }
        }
        if (!hasTitles) {
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
        Integer expectedSection = null;   // ожидаемый номер следующего раздела «N.»
        boolean prevLineWasListItem = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher m = HEADING_LINE.matcher(line);
            boolean isHeading = false;

            if (m.matches() && line.strip().length() <= MAX_HEADING_LENGTH) {
                String number = m.group(2);
                if (number == null) {
                    // markdown («#»), «Глава N», «§ N» — заголовок без доп. проверок
                    isHeading = true;
                } else if (isNumberedSectionHeading(number, expectedSection,
                        prevLineWasListItem, lines, i)) {
                    isHeading = true;
                    // Заголовок, разорванный переносом при извлечении из PDF,
                    // склеиваем со следующей строкой
                    StringBuilder title = new StringBuilder(line.strip());
                    int j = i + 1;
                    while (j < lines.length && !lines[j].isBlank()
                            && (title.toString().endsWith("-")
                            || Character.isLowerCase(lines[j].strip().charAt(0)))) {
                        if (!title.toString().endsWith("-")) {
                            title.append(' ');
                        }
                        title.append(lines[j].strip());
                        j++;
                    }
                    if (j > i + 1) {
                        // ФИКС: matcher заменяем только после успешного matches()
                        // на склеенной строке. Свежий Matcher без matches() при
                        // вызове group(1) бросает IllegalStateException — именно
                        // из-за него загрузка PDF с разорванными заголовками
                        // падала с сообщением «Не удалось прочитать файл».
                        Matcher merged = HEADING_LINE.matcher(title.toString());
                        if (merged.matches()) {
                            m = merged;
                            line = title.toString();
                            i = j - 1; // съеденные строки пропускаются основным циклом
                        }
                        // если склеенная строка не совпала с шаблоном — остаёмся
                        // на исходных m и line: продолжение уйдёт в текст урока
                    }
                    expectedSection = Integer.parseInt(number.split("\\.")[0]);
                } else {
                    // Совпало с шаблоном, но это пункт списка, а не заголовок
                    prevLineWasListItem = true;
                }
            } else {
                prevLineWasListItem = false;
            }

            if (isHeading) {
                String content = currentContent.toString().strip();
                if (!content.isEmpty()) {
                    if (currentTitle == null && result.isEmpty()) {
                        // Текст до первого заголовка — это вводка.
                        // Приклеим к первому уроку, а не выделяем в отдельный блок.
                        pendingIntro = content;
                    } else {
                        result.add(new Section(currentTitle, content, currentTitle != null));
                    }
                }
                currentTitle = cleanTitle(m.group(1));
                currentContent = new StringBuilder();
                prevLineWasListItem = false;
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

        // Вводка приклеивается в начало первого озаглавленного урока
        if (pendingIntro != null && !result.isEmpty()) {
            Section first = result.get(0);
            result.set(0, new Section(first.title, pendingIntro + "\n\n" + first.content, first.titled));
        } else if (pendingIntro != null) {
            // Страховка: заголовков не нашлось вовсе — вводка становится единственным блоком
            result.add(new Section(null, pendingIntro, false));
        }
        return result;
    }

    /**
     * «N. Название» — заголовок раздела, а не пункт списка.
     * <p>
     * Логика: раздел «1.» может быть первым; дальше номера идут по возрастанию
     * без пропусков (1, 2, ..., 11). Пункты списков внутри разделов начинаются
     * заново с 1 и потому нарушают монотонность; дополнительно пункт списка не
     * признаётся заголовком, если соседняя содержательная строка тоже нумерованная.
     * </p>
     */
    private boolean isNumberedSectionHeading(String number, Integer expected,
                                             boolean prevLineWasListItem, String[] lines, int index) {
        int n = Integer.parseInt(number.split("\\.")[0]);
        if (number.contains(".")) {
            // «1.2 Название» — всегда заголовок подраздела
            return true;
        }
        // Пункт списка: предыдущая или следующая содержательная строка тоже нумерованная
        if (prevLineWasListItem || nextMeaningfulLineIsNumbered(lines, index)) {
            return false;
        }
        return (expected == null && n == 1)              // первый раздел
                || (expected != null && n == expected + 1); // продолжение сквозной нумерации
    }

    /** Проверяет, является ли следующая непустая строка нумерованным пунктом. */
    private boolean nextMeaningfulLineIsNumbered(String[] lines, int index) {
        for (int j = index + 1; j < lines.length; j++) {
            String t = lines[j].strip();
            if (t.isEmpty()) {
                continue;
            }
            return HEADING_LINE.matcher(t).matches()
                    && NUMBERED_ITEM.matcher(t).matches();
        }
        return false;
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
                // Сбрасываем накопленное всегда, а не только при words >= MIN_WORDS.
                // Иначе куски большого абзаца попадают в результат раньше текста,
                // накопленного перед ним, — порядок чтения нарушается.
                if (current.length() > 0) {
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

    /** Перекрытие: 1–2 последних ПОЛНЫХ предложения предыдущего блока, не более OVERLAP_MAX_CHARS.
     *  Обрывки без точки (последняя строка, разорванная извлечением из PDF) в перекрытие
     *  не попадают: иначе они приклеиваются к первому предложению следующего блока
     *  и порождают дубли слов в его первом определении. */
    private String makeOverlap(String content) {
        String[] sentences = content.strip().split("(?<=[.!?…])\\s+");
        List<String> complete = new ArrayList<>();
        for (String s : sentences) {
            String t = s.strip();
            if (t.matches("(?s).+[.!?…]$")) {
                complete.add(t);
            }
        }
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, complete.size() - 2);
        for (int i = from; i < complete.size(); i++) {
            String s = complete.get(i);
            if (sb.length() + s.length() > OVERLAP_MAX_CHARS) break;
            if (sb.length() > 0) sb.append(' ');
            sb.append(s);
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