package ru.studybot.quiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Извлечение ключевых слов блока по TF-IDF относительно корпуса блоков одной книги.
 * <p>
 * TF — частота слова в текущем блоке, IDF — сглаженный обратный документный
 * логарифм по корпусу. Слова, встречающиеся во всех блоках, получают низкий вес
 * и в ключевые не попадают — это и есть фильтрация «общих» терминов.
 * Морфология не используется: приведение к нижнему регистру + отсечение стоп-слов.
 * </p>
 */
public class KeywordExtractor {

    private static final Pattern WORD = Pattern.compile("[а-яёa-z]{3,}");

    /** Стоп-слова: предлоги, союзы, местоимения, общие глаголы-связки. */
    public static final Set<String> STOP_WORDS = Set.copyOf(java.util.Arrays.asList(
            "и", "в", "во", "не", "что", "он", "на", "я", "с", "со", "как", "а", "то",
            "все", "она", "так", "его", "но", "да", "ты", "к", "у", "же", "вы", "за",
            "бы", "по", "только", "ее", "мне", "было", "вот", "от", "меня", "еще",
            "нет", "о", "из", "ему", "теперь", "когда", "даже", "ну", "ли", "если",
            "уже", "или", "ни", "быть", "был", "него", "до", "вас", "опять", "уж",
            "вам", "ведь", "там", "потом", "себя", "ничего", "ей", "может", "они",
            "тут", "где", "есть", "надо", "ней", "для", "мы", "тебя", "их", "чем",
            "была", "сам", "без", "будто", "чего", "раз", "тоже", "себе", "под",
            "будет", "тогда", "кто", "этот", "того", "потому", "этого", "какой",
            "совсем", "ним", "здесь", "этом", "один", "почти", "мой", "тем", "чтобы",
            "нее", "были", "куда", "зачем", "всех", "никогда", "можно", "при",
            "наконец", "два", "три", "об", "другой", "хоть", "после", "над", "больше",
            "тот", "через", "эти", "нас", "про", "всего", "них", "какая", "много",
            "разве", "эту", "моя", "впрочем", "хорошо", "свою", "этой", "перед",
            "иногда", "лучше", "чуть", "том", "нельзя", "такой", "им", "более",
            "всегда", "конечно", "всю", "между", "каждый", "например", "является",
            "являются", "который", "которые", "этим", "них", "обоих"
    ));

    /**
     * Возвращает до {@code limit} ключевых слов блока, отсортированных по убыванию веса.
     * Если корпус не передан, корпусом считается сам блок.
     */
    public List<String> extractKeywords(String blockText, List<String> allBlocks, int limit) {
        if (blockText == null || blockText.isBlank()) {
            return List.of();
        }
        List<String> corpus = (allBlocks == null || allBlocks.isEmpty()) ? List.of(blockText) : allBlocks;

        // Документная частота по всему корпусу
        Map<String, Integer> df = new HashMap<>();
        for (String block : corpus) {
            for (String term : terms(block)) {
                df.merge(term, 1, Integer::sum);
            }
        }

        // Частота в текущем блоке
        Map<String, Integer> tf = new HashMap<>();
        for (String term : terms(blockText)) {
            tf.merge(term, 1, Integer::sum);
        }

        int n = corpus.size();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(tf.entrySet());
        entries.sort((a, b) -> Double.compare(
                score(b.getKey(), b.getValue(), df, n),
                score(a.getKey(), a.getValue(), df, n)));

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entries) {
            if (result.size() >= limit) break;
            result.add(e.getKey());
        }
        return result;
    }

    /** TF-IDF: частота в блоке, умноженная на сглаженный IDF. */
    private double score(String term, int tf, Map<String, Integer> df, int n) {
        double idf = Math.log(1.0 + (double) n / (1.0 + df.getOrDefault(term, 0)));
        return tf * idf;
    }

    /** Токенизация: слова от 3 букв, нижний регистр, без стоп-слов. */
    static List<String> terms(String text) {
        List<String> result = new ArrayList<>();
        if (text == null) return result;
        Matcher m = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            String w = m.group();
            if (!STOP_WORDS.contains(w)) {
                result.add(w);
            }
        }
        return result;
    }
}
