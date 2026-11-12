package ru.studybot.repository;

import ru.studybot.model.Lesson;
import java.util.List;

public interface LessonRepository {
    /**
     * Сохраняет урок. Если урок с таким id уже есть — обновляет его (upsert).
     * Нужен для перезаписи урока с сгенерированными вопросами.
     */
    void save(Lesson lesson);
    List<Lesson> findAll();
    /** Все уроки указанной книги, отсортированные по номеру урока. */
    List<Lesson> findByBookId(String bookId);
}

