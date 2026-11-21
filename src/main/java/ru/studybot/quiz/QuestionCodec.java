package ru.studybot.quiz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.studybot.model.Question;

import java.util.List;

/**
 * Сериализация списка вопросов в JSON и обратно.
 * <p>
 * JSON хранится в поле Lesson.questions (строка). Джексон берётся общий,
 * отдельной конфигурации не требует: enum сериализуется именем,
 * вложенные списки — массивами строк.
 * </p>
 */
public final class QuestionCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private QuestionCodec() {} // только статические методы

    /** Сериализует список вопросов в JSON-строку для сохранения в Lesson. */
    public static String toJson(List<Question> questions) {
        try {
            return MAPPER.writeValueAsString(questions);
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка сериализации вопросов", e);
        }
    }

    /** Восстанавливает список вопросов из JSON-строки. Возвращает пустой список для пустых данных. */
    public static List<Question> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Question>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка десериализации вопросов", e);
        }
    }
}
