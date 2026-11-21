package ru.studybot.service;

import ru.studybot.model.LessonScore;
import ru.studybot.model.Question;
import ru.studybot.model.QuestionResult;
import ru.studybot.model.QuestionType;
import ru.studybot.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Сервис проверки ответов на вопросы и формирования итоговой оценки урока.
 * <p>
 * ИНТЕГРАЦИЯ С НАПАРНИКОМ:
 * Возвращаемый {@link LessonScore} передается напарнику для обновления
 * прогресса пользователя и принятия решения о доступности команды /next.
 * </p>
 */
public class QuizService {

    private final QuestionRepository questionRepository;

    public QuizService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Проверяет ответы пользователя и рассчитывает баллы.
     *
     * @param lessonId    ID урока
     * @param peerId      ID пользователя (для логирования/аудита)
     * @param userAnswers Map, где ключ — номер вопроса (String), значение — ответ пользователя
     * @return итоговая оценка урока {@link LessonScore}
     */
    public LessonScore checkAnswers(String lessonId, long peerId, Map<String, String> userAnswers) {
        List<Question> questions = questionRepository.findByLessonId(lessonId);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Для урока " + lessonId + " нет вопросов");
        }

        int totalScore = 0;
        int maxTotalScore = 0;
        List<QuestionResult> results = new ArrayList<>();

        for (Question q : questions) {
            maxTotalScore += q.getMaxScore();
            
            // Ключом в Map выступает номер вопроса (так как пользователь вводит "1=ответ", "2=ответ")
            String questionNumKey = String.valueOf(q.getQuestionNumber());
            String userAnswer = userAnswers.getOrDefault(questionNumKey, "").trim();
            
            boolean isCorrect = evaluateAnswer(q, userAnswer);
            
            // Пока реализуем систему "всё или ничего" за вопрос. 
            // В будущем можно добавить частичные баллы за SHORT_ANSWER.
            int score = isCorrect ? q.getMaxScore() : 0;
            totalScore += score;

            String correctAnswerStr = String.join(", ", q.getCorrectAnswers());
            
            results.add(new QuestionResult(q.getId(), isCorrect, score, userAnswer, correctAnswerStr));
        }

        return new LessonScore(totalScore, maxTotalScore);
    }

    /**
     * Умная проверка ответа с учетом типа вопроса.
     */
    private boolean evaluateAnswer(Question q, String userAnswer) {
        if (userAnswer.isEmpty()) return false;
        String lowerUser = userAnswer.toLowerCase();

        // Для краткого ответа проверяем наличие хотя бы одного ключевого слова (нечеткое сравнение)
        if (q.getType() == QuestionType.SHORT_ANSWER) {
            boolean hasKeyword = q.getKeywords().stream()
                    .anyMatch(kw -> lowerUser.contains(kw.toLowerCase()));
            if (hasKeyword) return true;
        }

        // Для вариантов ответа проверяем точное или частичное совпадение с эталоном
        return q.getCorrectAnswers().stream()
                .anyMatch(ca -> ca.toLowerCase().equals(lowerUser) || lowerUser.contains(ca.toLowerCase()));
    }
}