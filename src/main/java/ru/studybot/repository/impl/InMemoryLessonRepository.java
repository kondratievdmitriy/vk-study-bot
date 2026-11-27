package ru.studybot.repository.impl;

import ru.studybot.repository.LessonRepository;
import ru.studybot.model.Lesson;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InMemoryLessonRepository implements LessonRepository {
    private final List<Lesson> lessons = new ArrayList<>();

    @Override
    public void save(Lesson lesson) {
        // Upsert: если урок с таким id уже есть — обновляем, а не добавляем копию.
        // Нужен для перезаписи урока после генерации вопросов.
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
        // Интерфейс обещает сортировку по номеру урока
        result.sort(Comparator.comparingInt(Lesson::getLessonNumber));
        return result;
    }

    @Override
    public void deleteByBookId(String bookId) {
        lessons.removeIf(l -> l.getBookId().equals(bookId));
    }
}
