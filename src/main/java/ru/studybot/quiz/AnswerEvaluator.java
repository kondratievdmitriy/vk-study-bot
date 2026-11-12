package ru.studybot.quiz;

import ru.studybot.model.LessonScore;
import ru.studybot.model.Question;
import ru.studybot.model.QuestionResult;
import ru.studybot.model.QuestionType;

import java.util.List;

/**
 * Оценка ответов пользователя.
 * <p>
 * Выбор варианта и верно/неверно — точное сравнение текста выбранного
 * варианта с эталоном (регистр не учитывается). Краткий ответ — доля
 * ключевых слов эталона, найденных в ответе: 100% — максимум баллов,
 * от 50% — половина, меньше — ноль.
 * </p>
 */
public class AnswerEvaluator {

    private static final double FULL_MATCH_RATIO = 1.0;
    private static final double PARTIAL_MATCH_RATIO = 0.5;

    /** Оценивает один ответ. Некорректный ввод — это 0 баллов, а не исключение. */
    public QuestionResult evaluate(Question question, String userAnswer) {
        String answer = userAnswer == null ? "" : userAnswer.strip();
        String correctText = question.getCorrectAnswers().isEmpty()
                ? "" : question.getCorrectAnswers().get(0);

        if (question.getType() == QuestionType.MULTIPLE_CHOICE
                || question.getType() == QuestionType.TRUE_FALSE) {
            return evaluateChoice(question, answer, correctText);
        }
        return evaluateShortAnswer(question, answer, correctText);
    }

    private QuestionResult evaluateChoice(Question q, String answer, String correctText) {
        Integer optionNumber = parseOptionNumber(answer, q.getOptions().size());
        if (optionNumber == null) {
            return new QuestionResult(q.getId(), false, 0, answer, correctText);
        }
        String chosen = q.getOptions().get(optionNumber - 1);
        boolean correct = chosen.equalsIgnoreCase(correctText);
        return new QuestionResult(q.getId(), correct, correct ? q.getMaxScore() : 0, chosen, correctText);
    }

    private QuestionResult evaluateShortAnswer(Question q, String answer, String correctText) {
        if (answer.isEmpty()) {
            return new QuestionResult(q.getId(), false, 0, answer, correctText);
        }
        String normalized = normalize(answer);
        long matched = q.getKeywords().stream()
                .filter(k -> !k.isBlank())
                .filter(k -> normalized.contains(normalize(k)))
                .count();
        double ratio = q.getKeywords().isEmpty() ? 0 : (double) matched / q.getKeywords().size();

        int score;
        if (ratio >= FULL_MATCH_RATIO) {
            score = q.getMaxScore();
        } else if (ratio >= PARTIAL_MATCH_RATIO) {
            score = q.getMaxScore() / 2;
        } else {
            score = 0;
        }
        return new QuestionResult(q.getId(), score == q.getMaxScore(), score, answer, correctText);
    }

    /** Итог по уроку: пары «вопрос — результат» выравниваются по индексу.
     *  Возвращает null, если вопросов нет. */
    public LessonScore evaluateLesson(List<Question> questions, List<QuestionResult> results) {
        if (questions == null || questions.isEmpty()) {
            return null;
        }
        int total = 0;
        int max = 0;
        for (int i = 0; i < questions.size(); i++) {
            max += questions.get(i).getMaxScore();
            if (i < results.size()) {
                total += results.get(i).getScore();
            }
        }
        return new LessonScore(total, max);
    }

    /** Парсинг номера варианта: целое 1..max. Иначе null. */
    static Integer parseOptionNumber(String answer, int maxOption) {
        try {
            int n = Integer.parseInt(answer.strip());
            return (n >= 1 && n <= maxOption) ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Нормализация: нижний регистр, пунктуация -> пробелы, схлопывание пробелов. */
    static String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }
}
