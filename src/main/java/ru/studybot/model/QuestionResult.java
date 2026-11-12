package ru.studybot.model;

/**
 * Результат ответа пользователя на один вопрос.
 * <p>
 * Формируется AnswerEvaluator'ом и используется командой корректировки
 * плана для показа разбора ошибок (при условно засчитанном уроке).
 * </p>
 */
public class QuestionResult {
    private final String questionId;
    private final boolean correct;      // ответ засчитан полностью (maxScore баллов)?
    private final int score;            // фактически набранные баллы (0..maxScore)
    private final String userAnswer;    // что ответил пользователь
    private final String correctAnswer; // эталон — для разбора ошибок

    public QuestionResult(String questionId, boolean correct, int score,
                          String userAnswer, String correctAnswer) {
        this.questionId = questionId;
        this.correct = correct;
        this.score = score;
        this.userAnswer = userAnswer;
        this.correctAnswer = correctAnswer;
    }

    public String getQuestionId() { return questionId; }
    public boolean isCorrect() { return correct; }
    public int getScore() { return score; }
    public String getUserAnswer() { return userAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
}
