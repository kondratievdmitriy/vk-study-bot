package ru.studybot.repository.impl;

import ru.studybot.model.Lesson;
import ru.studybot.repository.LessonRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InMemoryLessonRepository implements LessonRepository {

    private final List<Lesson> lessons = new ArrayList<>();

    @Override
    public void save(Lesson lesson) {
        // upsert: заменяем существующий урок с тем же id
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).getId().equals(lesson.getId())) {
                lessons.set(i, lesson);
                return;
            }
        }
        lessons.add(lesson);
    }

    @Override
    public List<Lesson> findAll() {
        return List.copyOf(lessons);
    }

    @Override
    public List<Lesson> findByBookId(String bookId) {
        return lessons.stream()
                .filter(l -> bookId != null && bookId.equals(l.getBookId()))
                .sorted(Comparator.comparingInt(Lesson::getLessonNumber))
                .toList();
    }
}
