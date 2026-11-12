package ru.studybot.command;

import ru.studybot.model.Book;
import ru.studybot.model.Lesson;
import ru.studybot.model.LessonScore;
import ru.studybot.model.Question;
import ru.studybot.model.QuestionResult;
import ru.studybot.quiz.AnswerEvaluator;
import ru.studybot.quiz.QuestionCodec;
import ru.studybot.quiz.QuestionGenerator;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда /submit — принимает ответы на тест и оценивает усвоение урока.
 * <p>
 * Формат: /submit <название книги> <номер урока> | <ответы через ;>
 * Количество ответов должно совпадать с числом вопросов. Ответы выравниваются
 * по номерам вопросов: первый элемент — на вопрос 1 и т. д.
 * </p>
 */
public class SubmitCommand implements Command {

    private final BookRepository bookRepo;
    private final LessonRepository lessonRepo;
    private final AnswerEvaluator evaluator;
    private final QuestionGenerator generator;

    public SubmitCommand(BookRepository bookRepo, LessonRepository lessonRepo) {
        this(bookRepo, lessonRepo, new AnswerEvaluator(), new QuestionGenerator());
    }

    public SubmitCommand(BookRepository bookRepo, LessonRepository lessonRepo,
                         AnswerEvaluator evaluator, QuestionGenerator generator) {
        this.bookRepo = bookRepo;
        this.lessonRepo = lessonRepo;
        this.evaluator = evaluator;
        this.generator = generator;
    }

    @Override
    public String getName() {
        return "submit";
    }

    @Override
    public String getDescription() {
        return "Отправка ответов на тест по уроку";
    }

    @Override
    public String getDetailedHelp() {
        return "/submit <название книги> <номер урока> | <ответы через ;>\n"
                + "Число ответов должно совпадать с числом вопросов теста.\n"
                + "Для вопросов с вариантами пишите номер варианта, для кратких ответов — текст.";
    }

    @Override
    public String execute(MessageContext context) {
        String args = context.args() == null ? "" : context.args().strip();
        int separator = args.indexOf('|');
        if (separator == -1) {
            return "⚠️ Использование: /submit <название книги> <номер урока> | <ответы через ;>";
        }
        String[] titleAndNumber = QuizCommand.splitTitleAndNumber(args.substring(0, separator).strip());
        if (titleAndNumber == null) {
            return "⚠️ Использование: /submit <название книги> <номер урока> | <ответы через ;>";
        }
        Book book = bookRepo.findByTitle(titleAndNumber[0]);
        if (book == null) {
            return "❌ Книга \"" + titleAndNumber[0] + "\" не найдена.";
        }
        Lesson lesson = lessonRepo.findByBookId(book.getId()).stream()
                .filter(l -> l.getLessonNumber() == Integer.parseInt(titleAndNumber[1]))
                .findFirst().orElse(null);
        if (lesson == null) {
            return "❌ Урок №" + titleAndNumber[1] + " не найден в книге \"" + book.getTitle() + "\".";
        }

        List<Question> questions = QuestionCodec.fromJson(lesson.getQuestions());
        if (questions.isEmpty()) {
            questions = generator.generate(lesson, lessonRepo.findByBookId(book.getId()));
            if (questions.isEmpty()) {
                return "⚠️ По этому уроку нет вопросов — текст слишком короткий.";
            }
            lesson.setQuestions(QuestionCodec.toJson(questions));
            lessonRepo.save(lesson);
        }

        String[] answerParts = args.substring(separator + 1).split(";");
        List<String> answers = new ArrayList<>();
        for (String part : answerParts) {
            answers.add(part.strip());
        }
        if (answers.size() != questions.size()) {
            return "⚠️ Ответов: " + answers.size() + ", а вопросов: " + questions.size()
                    + ". Ответьте на все вопросы через \";\".";
        }

        List<QuestionResult> results = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            results.add(evaluator.evaluate(questions.get(i), answers.get(i)));
        }
        LessonScore score = evaluator.evaluateLesson(questions, results);
        return formatResult(lesson, questions, results, score);
    }

    private String formatResult(Lesson lesson, List<Question> questions,
                                List<QuestionResult> results, LessonScore score) {
        StringBuilder sb = new StringBuilder();
        sb.append("Результаты теста по уроку ").append(lesson.getLessonNumber())
                .append(": «").append(lesson.getTopic()).append("»\n\n");
        for (int i = 0; i < questions.size(); i++) {
            QuestionResult r = results.get(i);
            sb.append(questions.get(i).getQuestionNumber()).append(". ");
            if (r.isCorrect()) {
                sb.append("✅ Верно (").append(r.getScore()).append(" б.)\n");
            } else {
                sb.append("❌ Неверно. Правильный ответ: ").append(r.getCorrectAnswer()).append("\n");
            }
        }
        sb.append("\nИтог: ").append(score.getTotalScore()).append(" из ").append(score.getMaxTotalScore())
                .append(" (").append(score.percent()).append("%)\n\n");

        switch (score.getStatus()) {
            case PASSED -> sb.append("🎉 Урок засчитан! Переходите к следующему блоку: /next.");
            case CONDITIONALLY_PASSED -> sb.append("⚠️ Урок засчитан частично. Разбор ошибок выше; "
                    + "рекомендуем перечитать блок, затем /next.");
            case FAILED -> sb.append("📖 Урок не засчитан. Вернитесь к повторению материала: "
                    + "/learn выдаст этот блок снова.");
        }
        return sb.toString();
    }
}
