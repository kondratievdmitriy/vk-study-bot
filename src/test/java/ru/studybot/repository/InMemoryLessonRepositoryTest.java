package ru.studybot.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.studybot.model.Lesson;
import ru.studybot.repository.impl.InMemoryLessonRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLessonRepositoryTest {

    private InMemoryLessonRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryLessonRepository();
    }

    @Test
    void saveIsUpsertByLessonId() {
        Lesson lesson = new Lesson("b1", 1, "Тема", "Содержимое");
        repo.save(lesson);

        // Повторное сохранение того же урока (например, с записанными вопросами)
        lesson.setQuestions("[{\"text\":\"тест\"}]");
        repo.save(lesson);

        assertEquals(1, repo.findAll().size(), "Урок не должен дублироваться");
        assertEquals("[{\"text\":\"тест\"}]", repo.findAll().get(0).getQuestions());
    }

    @Test
    void findByBookIdFiltersAndSorts() {
        repo.save(new Lesson("b1", 2, "Урок 2", "..."));
        repo.save(new Lesson("b1", 1, "Урок 1", "..."));
        repo.save(new Lesson("b2", 1, "Чужой урок", "..."));

        List<Lesson> book1 = repo.findByBookId("b1");
        assertEquals(2, book1.size());
        assertEquals(1, book1.get(0).getLessonNumber(), "Уроки должны идти по номеру");
        assertEquals(2, book1.get(1).getLessonNumber());
    }

    @Test
    void findByBookIdUnknownReturnsEmpty() {
        assertTrue(repo.findByBookId("no-such-book").isEmpty());
    }
}
