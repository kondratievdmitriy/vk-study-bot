package ru.studybot.command;

import ru.studybot.model.Book;
import ru.studybot.model.Lesson;
import ru.studybot.model.Question;
import ru.studybot.quiz.QuestionCodec;
import ru.studybot.quiz.QuestionGenerator;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;

import java.util.List;

/**
 * Команда /quiz — показывает проверочный тест по уроку.
 * <p>
 * Вопросы генерируются один раз и сохраняются в Lesson.questions (JSON),
 * поэтому повторный вызов показывает тот же тест.
 * Формат ответов — см. SubmitCommand: /submit <книга> <урок> | <ответы через ;>
 * </p>
 */
public class QuizCommand implements Command {

    private final BookRepository bookRepo;
    private final LessonRepository lessonRepo;
    private final QuestionGenerator generator;

    public QuizCommand(BookRepository bookRepo, LessonRepository lessonRepo) {
        this(bookRepo, lessonRepo, new QuestionGenerator());
    }

    public QuizCommand(BookRepository bookRepo, LessonRepository lessonRepo, QuestionGenerator generator) {
        this.bookRepo = bookRepo;
        this.lessonRepo = lessonRepo;
        this.generator = generator;
    }

    @Override
    public String getName() {
        return "quiz";
    }

    @Override
    public String getDescription() {
        return "Проверочный тест по уроку";
    }

    @Override
    public String getDetailedHelp() {
        return "/quiz <название книги> <номер урока> — показать тест по уроку.\n"
                + "Ответы принимаются командой:\n"
                + "/submit <название книги> <номер урока> | <ответы через ;>\n"
                + "Для вопросов с вариантами — номер варианта; для кратких — свободный текст.\n"
                + "Пример: /submit Java ООП 1 | 2; 1; 3; Сокрытие деталей реализации";
    }

    @Override
    public String execute(MessageContext context) {
        String[] titleAndNumber = splitTitleAndNumber(context.args());
        if (titleAndNumber == null) {
            return "⚠️ Использование: /quiz <название книги> <номер урока>";
        }
        Book book = bookRepo.findByTitle(titleAndNumber[0]);
        if (book == null) {
            return "❌ Книга \"" + titleAndNumber[0] + "\" не найдена. Загрузите материал через /upload.";
        }
        Lesson lesson = findLesson(book.getId(), Integer.parseInt(titleAndNumber[1]));
        if (lesson == null) {
            return "❌ Урок №" + titleAndNumber[1] + " не найден в книге \"" + book.getTitle() + "\".";
        }

        List<Question> questions = QuestionCodec.fromJson(lesson.getQuestions());
        if (questions.isEmpty()) {
            questions = generator.generate(lesson, lessonRepo.findByBookId(book.getId()));
            if (questions.isEmpty()) {
                return "⚠️ По этому уроку не удалось составить вопросы — текст слишком короткий.";
            }
            lesson.setQuestions(QuestionCodec.toJson(questions));
            lessonRepo.save(lesson); // upsert: обновляем урок с сохранёнными вопросами
        }
        return formatQuiz(lesson, questions);
    }

    private Lesson findLesson(String bookId, int lessonNumber) {
        return lessonRepo.findByBookId(bookId).stream()
                .filter(l -> l.getLessonNumber() == lessonNumber)
                .findFirst()
                .orElse(null);
    }

    /** Последний токен аргументов — номер урока (число), остальное — название книги. */
    static String[] splitTitleAndNumber(String args) {
        if (args == null) return null;
        String[] tokens = args.strip().split("\\s+");
        if (tokens.length < 2) return null;
        String last = tokens[tokens.length - 1];
        if (!last.matches("\\d{1,4}")) return null;
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < tokens.length - 1; i++) {
            if (i > 0) title.append(' ');
            title.append(tokens[i]);
        }
        return new String[]{title.toString(), last};
    }

    private String formatQuiz(Lesson lesson, List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Тест по уроку ").append(lesson.getLessonNumber())
                .append(": «").append(lesson.getTopic()).append("»\n\n");
        for (Question q : questions) {
            sb.append(q.getQuestionNumber()).append(". ").append(q.getText()).append("\n");
            for (int i = 0; i < q.getOptions().size(); i++) {
                sb.append("   ").append(i + 1).append(") ").append(q.getOptions().get(i)).append("\n");
            }
            if (!q.getOptions().isEmpty()) {
                sb.append("   ").append(q.getType().getUserAnswerFormat()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("Ответьте одной командой:\n")
                .append("/submit ").append(lesson.getLessonNumber()).append("-й урок: <ответы через ;>\n")
                .append("Полный формат: /submit <название книги> <номер урока> | 2; 1; 3; ваш краткий ответ");
        return sb.toString();
    }

}
