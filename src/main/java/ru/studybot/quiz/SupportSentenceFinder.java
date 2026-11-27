package ru.studybot.quiz;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Поиск опорных предложений блока для генерации вопросов.
 * <p>
 * Приоритет определяется оценкой: предложения с маркерами определений
 * («называется», «является», «— это» и т. п.) получают бонус +10,
 * затем учитывается число ключевых слов в предложении.
 * Слишком короткие предложения (менее 20 символов) не рассматриваются.
 * </p>
 */
public class SupportSentenceFinder {

    private static final Pattern DEFINITION_MARKER = Pattern.compile(
            "называется|представляет собой|является|— это|называют",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final int MIN_SENTENCE_LENGTH = 20;
    private static final int DEFINITION_BONUS = 10;

    /** Возвращает опорные предложения, отсортированные по убыванию оценки. */
    public List<ScoredSentence> findSupportSentences(String text, List<String> keywords) {
        List<ScoredSentence> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        for (String raw : text.split("(?<=[.!?…])\\s+")) {
            // ФИКС: жёсткие переносы строк, оставшиеся после извлечения из PDF,
            // разрывают предложение на части. Границы предложений — только
            // [.!?…] + пробел, поэтому переносы внутри «предложения» — это
            // артефакт переноса строки, и их можно склеить в пробелы.
            // Без этого регулярки определений (`.` не матчит \n) не находили
            // ни одного определения, и /quiz отвечал «текст слишком короткий».
            String sentence = raw.replace('\n', ' ')
                    .replaceAll("\\s{2,}", " ")
                    .strip();
            if (sentence.length() < MIN_SENTENCE_LENGTH) {
                continue;
            }
            int keywordCount = 0;
            String lower = sentence.toLowerCase();
            for (String keyword : keywords) {
                if (lower.contains(keyword)) {
                    keywordCount++;
                }
            }
            boolean hasMarker = DEFINITION_MARKER.matcher(sentence).find();
            result.add(new ScoredSentence(sentence, (hasMarker ? DEFINITION_BONUS : 0) + keywordCount, hasMarker));
        }
        result.sort(Comparator.comparingInt(ScoredSentence::score).reversed());
        return result;
    }

    /** Опорное предложение с оценкой. */
    public record ScoredSentence(String text, int score, boolean hasDefinitionMarker) {}
}
