package ru.studybot.repository;

import ru.studybot.model.Lesson;
import java.util.List;

public interface LessonRepository {
    void save(Lesson lesson);
    List<Lesson> findAll();
}
