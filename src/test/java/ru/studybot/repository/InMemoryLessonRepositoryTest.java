package ru.studybot.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.studybot.model.Lesson;
import ru.studybot.repository.impl.InMemoryLessonRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты in-memory реализации LessonRepository: контракт, который PgLessonRepository
 * обязан повторять — upsert по lesson_id (без дубликатов), фильтрация по книге
 * с сортировкой по номеру урока, пустой результат для неизвестной книги.
 */
class InMemoryLessonRepositoryTest {

    private InMemoryLessonRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryLessonRepository();
    }

    /** Повторное сохранение того же урока (id не меняется) — обновление, а не дубль:
     *  так /upload перезаписывает вопросы после генерации, а /quiz записывает их в хранилище. */
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

    /** findByBookId возвращает только уроки запрошенной книги, упорядоченные по номеру
     *  независимо от порядка вставки. */
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

    /** Неизвестный bookId — пустой список, а не исключение: этот случай обычен
     *  для свежезапущенного бота без загруженных материалов. */
    @Test
    void findByBookIdUnknownReturnsEmpty() {
        assertTrue(repo.findByBookId("no-such-book").isEmpty());
    }
}
