package ru.studybot.repository;

import ru.studybot.model.Question;
import java.util.List;

/**
 * Интерфейс хранилища вопросов для тестов.
 * <p>
 * Определяет контракт для получения вопросов по уроку.
 * В текущей реализации используется in-memory хранилище,
 * но интерфейс позволяет легко заменить его на базу данных
 * (например, PostgreSQL или SQLite) без изменения бизнес-логики.
 * </p>
 *
 * @see Question
 * @see ru.studybot.repository.ru.studybot.InMemoryQuestionRepository
 */
public interface QuestionRepository {
    
    /**
     * Возвращает все вопросы, связанные с указанным уроком.
     * <p>
     * Вопросы используются командой {@link ru.studybot.command.QuizCommand}
     * для формирования теста и командой {@link ru.studybot.command.SubmitCommand}
     * для проверки ответов пользователя.
     * </p>
     *
     * @param lessonId уникальный идентификатор урока
     * @return список вопросов для урока; пустой список, если вопросов нет
     */
    List<Question> findByLessonId(String lessonId);
    
    /**
     * Возвращает общее количество вопросов для урока.
     * Используется для быстрой проверки, есть ли тест для данного урока.
     *
     * @param lessonId уникальный идентификатор урока
     * @return количество вопросов
     */
    int countByLessonId(String lessonId);
}
