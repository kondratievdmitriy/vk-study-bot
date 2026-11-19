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

    @Override
    public List<Lesson> findByBookId(String bookId) {
        List<Lesson> result = new ArrayList<>();
        for (Lesson l : lessons) {
            if (l.getBookId().equals(bookId)) {
                result.add(l);
            }
        }
        return result;
    }

    @Override
    public void deleteByBookId(String bookId) {
        lessons.removeIf(l -> l.getBookId().equals(bookId));
    }
}
