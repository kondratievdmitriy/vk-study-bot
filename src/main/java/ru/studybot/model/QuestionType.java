package ru.studybot.model;

/**
 * Тип проверочного вопроса.
 * <p>
 * Каждый тип определяет:
 * - формат ответа пользователя (см. userAnswerFormat);
 * - максимальный балл (maxScore);
 * - способ проверки (см. AnswerEvaluator).
 * </p>
 */
public enum QuestionType {

    /** Выбор одного варианта. Ответ пользователя: номер варианта (1–4). maxScore = 1. */
    MULTIPLE_CHOICE("Напишите номер варианта (1–4)", 1),

    /** Верно/неверно. Ответ пользователя: 1 (верно) или 2 (неверно). maxScore = 1. */
    TRUE_FALSE("Напишите 1 (верно) или 2 (неверно)", 1),

    /** Краткий свободный ответ. Ответ пользователя: свободный текст. maxScore = 2. */
    SHORT_ANSWER("Ответьте своими словами", 2);

    private final String userAnswerFormat;
    private final int maxScore;

    QuestionType(String userAnswerFormat, int maxScore) {
        this.userAnswerFormat = userAnswerFormat;
        this.maxScore = maxScore;
    }

    public String getUserAnswerFormat() { return userAnswerFormat; }
    public int getMaxScore() { return maxScore; }
}
