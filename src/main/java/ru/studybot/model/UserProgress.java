package ru.studybot.model;

import java.util.HashMap;
import java.util.Map;



/**
 * Прогресс пользователя по книге.
 * <p>
 * Хранит текущее состояние обучения: какая книга изучается,
 * на каком уроке остановился пользователь, и каков был результат
 * последнего теста (получен от напарника через LessonScore.Status).
 * </p>
 * <p>
 * ИНТЕГРАЦИЯ С НАПАРНИКОМ:
 * Поле {@code lastLessonStatus} заполняется после команды /submit напарника.
 * Команда /next читает это значение, чтобы решить — пускать пользователя
 * к следующему уроку или заставить повторить текущий.
 * </p>
 */
public class UserProgress {

    /** Статус обучения по книге. */
    public enum Status {
        NOT_STARTED,   // Книга ещё не начата
        STUDYING,      // Активное изучение
        REPEATING,     // Повторение после провала теста
        DONE           // Все уроки пройдены
    }

    private final long peerId;
    private String bookId;
    private int currentLessonNumber;
    private Status status;
    private LessonScore.Status lastLessonStatus; // Статус последнего теста (от напарника)
    private final Map<Integer, Integer> attemptsPerLesson; // Сколько раз пытался пройти каждый урок

    public UserProgress(long peerId) {
        this.peerId = peerId;
        this.currentLessonNumber = 1;
        this.status = Status.NOT_STARTED;
        this.attemptsPerLesson = new HashMap<>();
    }

    // --- Геттеры ---
    public long getPeerId() { return peerId; }
    public String getBookId() { return bookId; }
    public int getCurrentLessonNumber() { return currentLessonNumber; }
    public Status getStatus() { return status; }
    public LessonScore.Status getLastLessonStatus() { return lastLessonStatus; }
    public Map<Integer, Integer> getAttemptsPerLesson() { return attemptsPerLesson; }

    // --- Сеттеры ---
    public void setBookId(String bookId) { this.bookId = bookId; }
    public void setCurrentLessonNumber(int currentLessonNumber) { this.currentLessonNumber = currentLessonNumber; }
    public void setStatus(Status status) { this.status = status; }
    public void setLastLessonStatus(LessonScore.Status lastLessonStatus) { this.lastLessonStatus = lastLessonStatus; }

    /**
     * Увеличивает счётчик попыток для текущего урока.
     * Вызывается каждый раз, когда пользователь проходит тест (/submit).
     */
    public void incrementAttempts() {
        attemptsPerLesson.merge(currentLessonNumber, 1, Integer::sum);
    }

    /**
     * Возвращает количество попыток для указанного урока.
     */
    public int getAttemptsForLesson(int lessonNumber) {
        return attemptsPerLesson.getOrDefault(lessonNumber, 0);
    }

    /**
     * Переход к следующему уроку.
     */
    public void moveToNextLesson() {
        currentLessonNumber++;
    }

    /**
     * Сброс статуса после провала теста — пользователь должен повторить урок.
     */
    public void markAsRepeating() {
        this.status = Status.REPEATING;
    }

    /**
     * Пометить книгу как завершённую.
     */
    public void markAsDone() {
        this.status = Status.DONE;
    }

    @Override
    public String toString() {
        return "UserProgress{" +
                "peerId=" + peerId +
                ", bookId='" + bookId + '\'' +
                ", lesson=" + currentLessonNumber +
                ", status=" + status +
                ", lastTest=" + lastLessonStatus +
                '}';
    }
}