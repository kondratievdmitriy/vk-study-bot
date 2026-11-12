package ru.studybot.model;

/**
 * Итоговая оценка урока.
 * <p>
 * Пороги из плана проекта:
 * - PASSED:              процент ≥ 80% — блок засчитан, переход к следующему;
 * - CONDITIONALLY_PASSED: 60% ≤ процент < 80% — засчитан условно, показывается разбор ошибок;
 * - FAILED:               процент < 60% — не засчитан, возврат к повторению.
 * </p>
 * Статус читает напарник для корректировки плана (/next доступен при первых двух).
 */
public class LessonScore {

    public enum Status { PASSED, CONDITIONALLY_PASSED, FAILED }

    private final int totalScore;      // сумма баллов пользователя
    private final int maxTotalScore;   // максимально возможная сумма
    private final Status status;

    public LessonScore(int totalScore, int maxTotalScore) {
        if (maxTotalScore <= 0) {
            throw new IllegalArgumentException("maxTotalScore должен быть положительным");
        }
        if (totalScore < 0 || totalScore > maxTotalScore) {
            throw new IllegalArgumentException("totalScore вне диапазона [0, maxTotalScore]");
        }
        this.totalScore = totalScore;
        this.maxTotalScore = maxTotalScore;
        this.status = classify(percent());
    }

    /** Процент усвоения: 0–100. */
    public int percent() {
        return totalScore * 100 / maxTotalScore;
    }

    public int getTotalScore() { return totalScore; }
    public int getMaxTotalScore() { return maxTotalScore; }
    public Status getStatus() { return status; }

    private static Status classify(int percent) {
        if (percent >= 80) return Status.PASSED;
        if (percent >= 60) return Status.CONDITIONALLY_PASSED;
        return Status.FAILED;
    }

    @Override
    public String toString() {
        return "LessonScore{" + totalScore + "/" + maxTotalScore
                + " (" + percent() + "%), " + status + "}";
    }
}
