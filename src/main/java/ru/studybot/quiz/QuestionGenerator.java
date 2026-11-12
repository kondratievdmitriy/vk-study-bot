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
 * 2 верно/неверно (максимум 5 баллов). Если материала мало — генерируется
 * сколько есть, но не менее 2 вопросов; из совсем короткого текста вопросы
 * не генерируются вовсе.
 * </p>
 * <p>
 * Источники:
 * - краткий ответ и выбор варианта строятся из предложений-определений
 *   («X — это Y», «X называется Y», «X является Y»);
 * - верно/неверно — из тех же определений: чётные вопросы «истинные»,
 *   нечётные подменяют предикат дистрактором («X — это Z», где Z — термин
 *   из другого урока), чтобы нельзя было всегда отвечать «Верно».
 * </p>
 */
public class QuestionGenerator {

    private final KeywordExtractor keywordExtractor = new KeywordExtractor();
    private final SupportSentenceFinder sentenceFinder = new SupportSentenceFinder();

    private static final Pattern DEF_DASH = Pattern.compile("(?i)^(.{3,80}?)\\s+—\\s+это\\s+(.+?)[.?!]?$");
    private static final Pattern DEF_NAMED = Pattern.compile("(?i)^(.{3,80}?)\\s+называется\\s+(.+?)[.?!]?$");
    private static final Pattern DEF_IS = Pattern.compile("(?i)^(.{3,80}?)\\s+является\\s+(.+?)[.?!]?$");
    private static final List<String> TRUE_FALSE_OPTIONS = List.of("Верно", "Неверно");
    private static final int REQUIRED_DISTRACTORS = 3;
    private static final int MAX_TRUE_FALSE = 2;

    /**
     * Генерирует вопросы по уроку. Результат детерминирован
     * (позиция правильного варианта сдвигается по хешу subject, а не случайностью).
     */
    public List<Question> generate(Lesson lesson, List<Lesson> allLessons) {
        List<Question> result = new ArrayList<>();
        if (lesson == null || lesson.getContent() == null || lesson.getContent().isBlank()) {
            return result;
        }
        List<String> corpus = new ArrayList<>();
        if (allLessons != null) {
            for (Lesson l : allLessons) {
                corpus.add(l.getContent());
            }
        }
        List<String> keywords = keywordExtractor.extractKeywords(lesson.getContent(), corpus, 8);
        List<SupportSentenceFinder.ScoredSentence> sentences =
                sentenceFinder.findSupportSentences(lesson.getContent(), keywords);
        if (sentences.isEmpty()) {
            return result;
        }
        List<String> distractorPool = collectDistractors(allLessons, lesson, keywords);

        int number = 1;
        String[] definition = findDefinition(sentences);

        if (definition != null) {
            // Краткий ответ
            result.add(new Question(number++, QuestionType.SHORT_ANSWER,
                    "Кратко: что такое " + definition[0] + "?",
                    List.of(),
                    List.of(capitalize(definition[1])),
                    KeywordExtractor.terms(definition[1])));

            // Выбор варианта: те же 4 опоры, что у краткого ответа, плюс дистракторы
            List<String> options = buildOptions(definition[1], distractorPool);
            if (options.size() == 1 + REQUIRED_DISTRACTORS) {
                result.add(new Question(number++, QuestionType.MULTIPLE_CHOICE,
                        "Что такое " + definition[0] + "?",
                        options,
                        List.of(definition[1]),
                        List.of()));
            }

            // Верно/неверно по найденным определениям
            int tfCount = 0;
            for (SupportSentenceFinder.ScoredSentence s : sentences) {
                if (tfCount >= MAX_TRUE_FALSE) break;
                String[] def = parseDefinition(s.text());
                if (def == null) continue;
                boolean makeFalse = (tfCount % 2 == 1);
                String predicate = def[1];
                if (makeFalse) {
                    String distractor = pickDistractor(distractorPool, List.of(def[1]));
                    if (distractor == null) continue;
                    predicate = distractor;
                }
                result.add(new Question(number++, QuestionType.TRUE_FALSE,
                        def[0] + " — это " + predicate + ". Верно ли это утверждение?",
                        TRUE_FALSE_OPTIONS,
                        List.of(makeFalse ? "Неверно" : "Верно"),
                        List.of()));
                tfCount++;
            }
        }
        return result;
    }

    /** Ищет первое предложение-определение среди опорных. */
    private String[] findDefinition(List<SupportSentenceFinder.ScoredSentence> sentences) {
        for (SupportSentenceFinder.ScoredSentence s : sentences) {
            String[] def = parseDefinition(s.text());
            if (def != null) {
                return def;
            }
        }
        return null;
    }

    /** Разбор определения. Возвращает {субъект, предикат} или null. */
    String[] parseDefinition(String sentence) {
        for (Pattern p : List.of(DEF_DASH, DEF_NAMED, DEF_IS)) {
            Matcher m = p.matcher(sentence.strip());
            if (m.matches()) {
                return new String[]{m.group(1).strip(), m.group(2).strip()};
            }
        }
        return null;
    }

    /** Пул дистракторов: ключевые слова других уроков + прочие слова текущего блока. */
    private List<String> collectDistractors(List<Lesson> allLessons, Lesson current, List<String> currentKeywords) {
        LinkedHashSet<String> pool = new LinkedHashSet<>();
        if (allLessons != null) {
            for (Lesson l : allLessons) {
                if (l == current) continue;
                pool.addAll(KeywordExtractor.terms(l.getContent()));
            }
        }
        Set<String> keywordSet = new HashSet<>(currentKeywords);
        for (String term : KeywordExtractor.terms(current.getContent())) {
            if (!keywordSet.contains(term)) {
                pool.add(term);
            }
        }
        return new ArrayList<>(pool);
    }

    /** Первый дистрактор, не пересекающийся с исключениями (по вхождению подстроки). */
    private String pickDistractor(List<String> pool, List<String> excluded) {
        for (String d : pool) {
            boolean conflicts = false;
            for (String e : excluded) {
                String dl = d.toLowerCase();
                String el = e.toLowerCase();
                if (el.contains(dl) || dl.contains(el)) {
                    conflicts = true;
                    break;
                }
            }
            if (!conflicts) {
                return capitalize(d);
            }
        }
        return null;
    }

    /** 4 варианта: правильный + 3 дистрактора. Позиция правильного сдвигается детерминированно. */
    private List<String> buildOptions(String correct, List<String> pool) {
        List<String> distractors = new ArrayList<>();
        for (String d : pool) {
            if (distractors.size() >= REQUIRED_DISTRACTORS) break;
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
        options.add(correct);
        options.addAll(distractors);
        Collections.rotate(options, Math.floorMod(correct.hashCode(), options.size()));
        return options;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
