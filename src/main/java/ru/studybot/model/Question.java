package ru.studybot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;

/**
 * Проверочный вопрос по уроку.
 * <p>
 * Создаётся генератором вопросов (задача 3) один раз при загрузке материала,
 * хранится в {@link Lesson#getQuestions()} в виде JSON. Восстанавливается
 * через {@code QuestionCodec}.
 * </p>
 * <p>
 * Заполнение полей зависит от типа:
 * - MULTIPLE_CHOICE: options — 4 варианта, correctAnswers — 1 элемент;
 * - TRUE_FALSE: options — ["Верно", "Неверно"], correctAnswers — 1 элемент;
 * - SHORT_ANSWER: options пуст, correctAnswers — эталонный ответ,
 *   keywords — ключевые слова эталона для нечёткого сравнения.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true) // устойчивость к будущим полям
public class Question {
    private String id;                  // UUID
    private int questionNumber;         // номер в рамках урока, с 1
    private QuestionType type;
    private String text;                // текст вопроса
    private List<String> options;       // варианты ответа (для choice и true/false)
    private List<String> correctAnswers; // правильный ответ(ы)
    private List<String> keywords;      // ключевые слова эталона (для краткого ответа)
    private int maxScore;               // 1 или 2 (дублирует type.getMaxScore())

    /** Пустой конструктор для Jackson. Не используйте напрямую. */
    public Question() {}

    public Question(int questionNumber, QuestionType type, String text,
                    List<String> options, List<String> correctAnswers, List<String> keywords) {
        this.id = java.util.UUID.randomUUID().toString();
        this.questionNumber = questionNumber;
        this.type = type;
        this.text = text;
        this.options = options;
        this.correctAnswers = correctAnswers;
        this.keywords = keywords;
        this.maxScore = type.getMaxScore();
    }

    public String getId() { return id; }
    public int getQuestionNumber() { return questionNumber; }
    public QuestionType getType() { return type; }
    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public List<String> getCorrectAnswers() { return correctAnswers; }
    public List<String> getKeywords() { return keywords; }
    public int getMaxScore() { return maxScore; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question)) return false;
        Question q = (Question) o;
        return questionNumber == q.questionNumber
                && maxScore == q.maxScore
                && type == q.type
                && Objects.equals(text, q.text)
                && Objects.equals(options, q.options)
                && Objects.equals(correctAnswers, q.correctAnswers)
                && Objects.equals(keywords, q.keywords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionNumber, type, text, options, correctAnswers, keywords);
    }
}
