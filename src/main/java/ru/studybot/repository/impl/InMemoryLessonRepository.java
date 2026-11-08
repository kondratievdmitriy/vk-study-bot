package ru.studybot.repository.impl;

import ru.studybot.repository.LessonRepository;
import ru.studybot.model.Lesson;
import java.util.*;

public class InMemoryLessonRepository implements LessonRepository {
    private final List<Lesson> lessons = new ArrayList<>();

    @Override
    public void save(Lesson lesson) {
        lessons.add(lesson);
    }

    @Override
    public List<Lesson> findAll() {
        return new ArrayList<>(lessons);
    }
}
