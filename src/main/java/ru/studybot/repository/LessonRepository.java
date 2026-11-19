package ru.studybot.repository;

import ru.studybot.model.Lesson;
import java.util.List;

public interface LessonRepository {
    void save(Lesson lesson);
    List<Lesson> findAll();

    /** Все уроки конкретной книги. */
    List<Lesson> findByBookId(String bookId);

    /** Удаляет все уроки книги (используется при повторной загрузке материала). */
    void deleteByBookId(String bookId);
}
