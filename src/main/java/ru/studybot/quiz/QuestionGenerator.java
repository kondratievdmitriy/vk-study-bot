package ru.studybot.quiz;

import ru.studybot.model.Lesson;
import ru.studybot.model.Question;
import ru.studybot.model.QuestionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Генератор проверочных вопросов по уроку на основе шаблонов.
 * <p>
 * Целевой состав теста на блок: 1 краткий ответ + 1 выбор варианта +
 * до 2 верно/неверно. Если материала мало — генерируется сколько есть;
 * из совсем короткого текста вопросы не генерируются вовсе.
 * </p>
 * <p>
 * Определения распознаются в четырёх формах: «X — это Y», «X называется Y»,
 * «X является Y» и «X: Y» (пункты списков, типичные для учебных PDF).
 * Краткий ответ — по первому определению, выбор варианта — по следующему
 * с другим субъектом, верно/неверно — сначала по неиспользованным
 * определениям, затем по использованным (запас), при нехватке — по прочим
 * содержательным предложениям (не заголовки и не обрывки списков).
 * </p>
 * <p>
 * Ложные утверждения верно/неверно строятся отрицанием связки определения
 * («X — это не Y», «X не является Y»); перечисление через «или» под
 * отрицанием разворачивается в «не X и не Y». Для предложений без распознанной
 * связки используется обёртка «Неверно, что …».
 * </p>
 * <p>
 * Системная нормализация: контент урока выпрямляется один раз на входе
 * генератора (все Unicode-пробелы — к обычному пробелу, невидимые символы
 * нулевой ширины — удаляются). Схлопывание повторов работает только
 * на границах слов.
 * </p>
 */
public class QuestionGenerator {

    private final KeywordExtractor keywordExtractor = new KeywordExtractor();
    private final SupportSentenceFinder sentenceFinder = new SupportSentenceFinder();

    private static final Pattern DEF_DASH = Pattern.compile("(?i)^(.{3,80}?)\\s+—\\s+это\\s+(.+?)[.?!]?$");
    private static final Pattern DEF_NAMED = Pattern.compile("(?i)^(.{3,80}?)\\s+называется\\s+(.+?)[.?!]?$");
    private static final Pattern DEF_IS = Pattern.compile("(?i)^(.{3,80}?)\\s+является\\s+(.+?)[.?!]?$");
    /** Определение через двоеточие — типичная форма пунктов списков в учебных PDF. */
    private static final Pattern DEF_COLON = Pattern.compile("^(.{3,80}?):\\s+(.+?)[.?!]?$");
    private static final List<String> TRUE_FALSE_OPTIONS = List.of("Верно", "Неверно");
    private static final int REQUIRED_DISTRACTORS = 3;
    private static final int MAX_TRUE_FALSE = 2;
    /** Дистрактор короче этого значения («Это», «Что») выглядит мусором. */
    private static final int MIN_DISTRACTOR_LENGTH = 4;

    /** Маркер списка в начале строки: «- », «• », «1. », «1) ». */
    private static final Pattern LEADING_BULLET = Pattern.compile("^(?:[-•*—]|\\d+[.)])\\s+");

    /** Служебные вступления перед субъектом: «Вывод:», «Итак,», «Таким образом,» и т. п. */
    private static final Pattern LEADING_DISCOURSE = Pattern.compile(
            "(?iu)^(вывод\\s*:|итак[,:]?|таким\\s+образом[,:]?|следовательно[,:]?|итого[,:]?)\\s*");

    /** Хвостовая пунктуация субъекта/предиката: запятая перед «— это» и т. п. */
    private static final Pattern TRAILING_PUNCT = Pattern.compile("[,:;—–-]+\\s*$");

    /**
     * Любой Unicode-пробел (NBSP, узкие пробелы, U+2000–200A, U+3000 и т. д.).
     * Флаг UNICODE_CHARACTER_CLASS расширяет \s на всю категорию пробельных —
     * обычный \s в Java покрывает лишь ASCII.
     */
    private static final Pattern ANY_WHITESPACE =
            Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * Схлопывание повторяющихся подряд слов, только на границах слов.
     * Lookbehind/lookahead по буквам обязательны: без них регулярка
     * матчит одиночную букву ВНУТРИ слова — «фон Неймана» превращалась
     * в «фонеймана» (н + Н сворачивались как дубль).
     */
    private static final Pattern REPEATED_WORD = Pattern.compile(
            "(?iu)(?<![а-яёa-z])([а-яёa-z][а-яёa-z-]*)(?![а-яёa-z])(?:\\s+\\1(?![а-яёa-z]))+");

    /** Распознанное определение: исходное предложение + вычищенные субъект и предикат. */
    private record Definition(SupportSentenceFinder.ScoredSentence source, String subject, String predicate) {}

    /**
     * Генерирует вопросы по уроку. Результат детерминирован
     * (позиция правильного варианта сдвигается по хешу subject, а не случайностью).
     */
    public List<Question> generate(Lesson lesson, List<Lesson> allLessons) {
        List<Question> result = new ArrayList<>();
        if (lesson == null || lesson.getContent() == null || lesson.getContent().isBlank()) {
            return result;
        }
        // СИСТЕМНАЯ НОРМАЛИЗАЦИЯ: контент урока выпрямляется один раз, на входе.
        // Все Unicode-пробелы (NBSP и родственные) заменяются обычным пробелом,
        // невидимые нулевой ширины — удаляются. Ниже по цепочке NBSP физически
        // не встречается: ни в разборе определений, ни в чистке субъектов,
        // ни в утверждениях верно/неверно.
        String content = normalizeSpaces(lesson.getContent());
        List<String> corpus = new ArrayList<>();
        if (allLessons != null) {
            for (Lesson l : allLessons) {
                corpus.add(l.getContent() == null ? "" : normalizeSpaces(l.getContent()));
            }
        }
        List<String> keywords = keywordExtractor.extractKeywords(content, corpus, 8);
        List<SupportSentenceFinder.ScoredSentence> sentences =
                sentenceFinder.findSupportSentences(content, keywords);
        if (sentences.isEmpty()) {
            return result;
        }
        List<String> distractorPool = collectDistractors(allLessons, lesson, keywords);

        // 1. Собираем все распознанные определения с вычищенными субъектом/предикатом
        List<Definition> defs = new ArrayList<>();
        Set<String> seenFullText = new HashSet<>();
        for (SupportSentenceFinder.ScoredSentence s : sentences) {
            String[] parts = parseDefinition(s.text());
            if (parts == null) continue;
            String subject = cleanSubject(parts[0]);
            String predicate = cleanPredicate(parts[1]);
            if (subject.isBlank() || predicate.isBlank()) continue;
            if (!seenFullText.add(s.text().toLowerCase())) continue; // защита от дублей предложений
            defs.add(new Definition(s, subject, predicate));
        }
        if (defs.isEmpty()) {
            return result;
        }

        int number = 1;

        // 2. Краткий ответ — по первому определению
        Definition first = defs.get(0);
        result.add(new Question(number++, QuestionType.SHORT_ANSWER,
                "Кратко: что такое " + first.subject() + "?",
                List.of(),
                List.of(capitalize(first.predicate())),
                KeywordExtractor.terms(first.predicate())));

        // 3. Выбор варианта — по следующему определению с ДРУГИМ субъектом.
        // Если второго субъекта нет — вопрос не дублируем, а пропускаем.
        Definition choiceDef = null;
        for (Definition d : defs.subList(1, defs.size())) {
            if (!d.subject().equalsIgnoreCase(first.subject())) {
                choiceDef = d;
                break;
            }
        }
        if (choiceDef != null) {
            // Дистракторы: сначала предикаты других определений блока — они
            // содержательно конкурируют с правильным ответом; затем термины
            // текущего урока, затем термины других уроков.
            LinkedHashSet<String> pool = new LinkedHashSet<>();
            for (Definition d : defs) {
                if (d != choiceDef) {
                    pool.add(d.predicate());
                }
            }
            pool.addAll(distractorPool);
            List<String> options = buildOptions(choiceDef.predicate(), new ArrayList<>(pool));
            if (options.size() == 1 + REQUIRED_DISTRACTORS) {
                result.add(new Question(number++, QuestionType.MULTIPLE_CHOICE,
                        "Что такое " + choiceDef.subject() + "?",
                        options,
                        List.of(choiceDef.predicate()),
                        List.of()));
            }
        }

        // 4. Верно/неверно: сначала неиспользованные определения, затем
        //    использованные — как запас; при нехватке определений — прочие
        //    содержательные предложения (не заголовки, не обрывки списков).
        Set<String> tfUsed = new HashSet<>();
        tfUsed.add(first.source().text().toLowerCase());
        if (choiceDef != null) {
            tfUsed.add(choiceDef.source().text().toLowerCase());
        }
        List<SupportSentenceFinder.ScoredSentence> tfSources = new ArrayList<>();
        for (Definition d : defs) {
            if (!tfUsed.contains(d.source().text().toLowerCase())) {
                tfSources.add(d.source());
            }
        }
        for (Definition d : defs) {
            if (!tfSources.contains(d.source())) {
                tfSources.add(d.source());
            }
        }
        for (SupportSentenceFinder.ScoredSentence s : sentences) {
            if (tfSources.contains(s)) continue;
            String t = s.text().strip();
            if (parseDefinition(t) != null) continue;         // уже учтено в defs
            if (t.split("\\s+").length < 5) continue;          // обрывки и заголовки
            if (t.endsWith(":")) continue;                     // заголовки разделов
            if (t.matches("(?s).*\\d+\\.\\s.*")) continue;     // склейки нумерованных списков
            tfSources.add(s);
        }

        int tfCount = 0;
        for (SupportSentenceFinder.ScoredSentence s : tfSources) {
            if (tfCount >= MAX_TRUE_FALSE) break;
            String source = s.text().strip();
            // Порядок tfSources уже гарантирует приоритет: сначала неиспользованные
            // определения, затем использованные (запас), затем прочие предложения.
            // Повторное использование допустимо — это запасной сценарий тесного блока.
            String base = stripDiscourse(source);
            if (base.isBlank()) continue;
            boolean makeFalse = (tfCount % 2 == 1);
            String statement;
            if (makeFalse) {
                statement = negate(base);
                if (statement == null) {
                    statement = "Неверно, что " + base;
                }
            } else {
                statement = base;
            }
            result.add(new Question(number++, QuestionType.TRUE_FALSE,
                    statement + " Верно ли это утверждение?",
                    TRUE_FALSE_OPTIONS,
                    List.of(makeFalse ? "Неверно" : "Верно"),
                    List.of()));
            tfUsed.add(source.toLowerCase());
            tfCount++;
        }
        return result;
    }

    /** Разбор определения. Возвращает {субъект, предикат} или null. */
    String[] parseDefinition(String sentence) {
        String s = LEADING_BULLET.matcher(sentence.strip()).replaceFirst("").strip();
        for (Pattern p : List.of(DEF_DASH, DEF_NAMED, DEF_IS, DEF_COLON)) {
            Matcher m = p.matcher(s);
            if (m.matches()) {
                String subject = m.group(1).strip();
                String predicate = m.group(2).strip();
                // Списочные обрывки: «X: 1. …» или «X: - …» — не определения
                if (predicate.matches("\\d.*") || predicate.matches("^[-–—]\\s.*")) {
                    return null;
                }
                return new String[]{subject, predicate};
            }
        }
        return null;
    }

    /**
     * Приводит ЛЮБОЙ Unicode-пробел (NBSP, узкие пробелы, полные и т. д.)
     * к обычному, а невидимые нулевой ширины (ZWSP, word joiner, BOM) — удаляет.
     * <p>
     * NBSP и родственные символы регулярно встречаются в текстах из PDF; strip()
     * и обычный \s их не считают пробелами.
     * </p>
     */
    static String normalizeSpaces(String s) {
        return ANY_WHITESPACE.matcher(s).replaceAll(" ")
                .replace("\u200B", "")
                .replace("\u2060", "")
                .replace("\uFEFF", "");
    }

    /**
     * Срезает маркер списка («- », «1. ») и служебное вступление («Вывод:»,
     * «Итак,» и т. п.) с начала строки. Используется при чистке субъекта и при
     * построении утверждений верно/неверно.
     */
    static String stripDiscourse(String sentence) {
        String s = LEADING_BULLET.matcher(normalizeSpaces(sentence.strip())).replaceFirst("").strip();
        return LEADING_DISCOURSE.matcher(s).replaceFirst("").strip();
    }

    /**
     * Чистка субъекта определения:
     * 1) срезает маркеры списков и служебные вступления;
     * 2) схлопывает повторяющиеся подряд слова ТОЛЬКО на границах слов
     *    («X X» -> «X» — артефакт PDF; «фон Неймана» не трогает);
     * 3) убирает хвостовую пунктуацию («…Неймана,» -> «…Неймана»).
     */
    static String cleanSubject(String subject) {
        String s = stripDiscourse(subject.strip());
        s = REPEATED_WORD.matcher(s).replaceAll("$1");
        s = TRAILING_PUNCT.matcher(s).replaceAll("").strip();
        return s;
    }

    /** Чистка предиката: нормализует пробелы и убирает хвостовую пунктуацию. */
    static String cleanPredicate(String predicate) {
        return TRAILING_PUNCT.matcher(normalizeSpaces(predicate.strip()))
                .replaceAll("").strip();
    }

    /**
     * Строит ложное утверждение из исходного: отрицание добавляется к связке
     * определения («— это» -> «— это не», «является» -> «не является»).
     * Перечисление через «или» под отрицанием грамматически корректно
     * разворачивается: «не A или B» -> «не A и не B».
     * Возвращает null, если предложение не является распознаваемым определением.
     */
    String negate(String sentence) {
        String s = sentence.strip();
        String negated = null;
        if (DEF_DASH.matcher(s).matches()) {
            negated = s.replaceFirst("(?i)—\\s*это", "— это не");
        } else {
            for (Pattern p : List.of(DEF_NAMED, DEF_IS)) {
                if (p.matcher(s).matches()) {
                    negated = s.replaceFirst("(?i)(называется|является|представляет\\s+собой)", "не $1");
                    break;
                }
            }
        }
        if (negated == null) {
            return null;
        }
        // «не программная или аппаратная среда» -> «не программная и не аппаратная среда»
        return negated.replaceFirst("(?iu)не\\s+([^,.;:!?]+?)\\s+или\\s+", "не $1 и не ");
    }

    /**
     * Пул дистракторов: сначала термины текущего блока (не ключевые) —
     * они остаются в теме урока, затем термины других уроков
     * (межпредметная проверка).
     */
    private List<String> collectDistractors(List<Lesson> allLessons, Lesson current, List<String> currentKeywords) {
        LinkedHashSet<String> pool = new LinkedHashSet<>();
        Set<String> keywordSet = new HashSet<>(currentKeywords);
        String currentContent = current.getContent() == null ? "" : normalizeSpaces(current.getContent());
        for (String term : KeywordExtractor.terms(currentContent)) {
            if (!keywordSet.contains(term)) {
                pool.add(term);
            }
        }
        if (allLessons != null) {
            for (Lesson l : allLessons) {
                if (l == current) continue;
                pool.addAll(KeywordExtractor.terms(l.getContent() == null ? "" : normalizeSpaces(l.getContent())));
            }
        }
        return new ArrayList<>(pool);
    }

    /** 4 варианта: правильный + 3 дистрактора. Позиция правильного сдвигается детерминированно. */
    private List<String> buildOptions(String correct, List<String> pool) {
        List<String> distractors = new ArrayList<>();
        for (String d : pool) {
            if (distractors.size() >= REQUIRED_DISTRACTORS) break;
            if (d.length() < MIN_DISTRACTOR_LENGTH) continue;
            String dl = d.toLowerCase();
            String cl = correct.toLowerCase();
            if (!cl.contains(dl) && !dl.contains(cl)) {
                distractors.add(capitalize(d));
            }
        }
        if (distractors.size() < REQUIRED_DISTRACTORS) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        options.add(capitalize(correct));
        options.addAll(distractors);
        Collections.rotate(options, Math.floorMod(correct.hashCode(), options.size()));
        return options;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
