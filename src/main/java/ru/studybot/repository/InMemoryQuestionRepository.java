package ru.studybot.repository;

import ru.studybot.model.Question;
import ru.studybot.model.QuestionType;

import java.util.*;

/**
 * In-memory реализация {@link QuestionRepository}.
 * Хранит вопросы в памяти приложения для демонстрации и тестирования.
 */
public class InMemoryQuestionRepository implements QuestionRepository {
    
    private final Map<String, List<Question>> questionsByLesson;

    public InMemoryQuestionRepository() {
        this.questionsByLesson = new HashMap<>();
        initializeDemoQuestions();
    }

    private void initializeDemoQuestions() {
        // --- Вопросы для урока 1 (lesson_01) ---
        List<Question> lesson01Questions = new ArrayList<>();
        
        // Вопрос 1: Выбор одного варианта (MULTIPLE_CHOICE в твоей логике, или SINGLE_CHOICE)
        lesson01Questions.add(new Question(
                1, // questionNumber
                QuestionType.MULTIPLE_CHOICE, // или QuestionType.SINGLE_CHOICE, если есть такой enum
                "Что такое инкапсуляция?",
                Arrays.asList("Скрытие внутреннего состояния объекта", "Наследование свойств", "Создание объектов", "Вызов методов"),
                Collections.singletonList("Скрытие внутреннего состояния объекта"), // correctAnswers как List
                Collections.singletonList("инкапсуляция") // keywords
        ));

        // Вопрос 2: Краткий ответ (SHORT_ANSWER)
        lesson01Questions.add(new Question(
                2,
                QuestionType.SHORT_ANSWER,
                "Назовите три основных принципа ООП.",
                Collections.emptyList(), // options пуст для краткого ответа
                Collections.singletonList("инкапсуляция, наследование, полиморфизм"), // эталонный ответ
                Arrays.asList("инкапсуляция", "наследование", "полиморфизм") // keywords для нечёткого сравнения
        ));

        questionsByLesson.put("lesson_01", lesson01Questions);

        // --- Вопросы для урока 2 (lesson_02) ---
        List<Question> lesson02Questions = new ArrayList<>();
        
        lesson02Questions.add(new Question(
                1,
                QuestionType.TRUE_FALSE, // Или MULTIPLE_CHOICE
                "Класс является шаблоном для создания объектов.",
                Arrays.asList("Верно", "Неверно"),
                Collections.singletonList("Верно"),
                Collections.singletonList("верно")
        ));

        questionsByLesson.put("lesson_02", lesson02Questions);
    }

    @Override
    public List<Question> findByLessonId(String lessonId) {
        return questionsByLesson.getOrDefault(lessonId, Collections.emptyList());
    }

    @Override
    public int countByLessonId(String lessonId) {
        return questionsByLesson.getOrDefault(lessonId, Collections.emptyList()).size();
    }
}