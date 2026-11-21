package ru.studybot.command;

import ru.studybot.model.Book;
import ru.studybot.model.Lesson;
import ru.studybot.model.LessonScore;
import ru.studybot.model.UserProgress;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.repository.UserProgressRepository;

import java.util.List;

/**
 * Команда /next — переход к следующему уроку или возврат к повторению.
 * <p>
 * Реализует Задачу 3, пункт 4: "Корректировать план: возврат к повторению 
 * или переход к следующему блоку".
 * </p>
 * <p>
 * ИНТЕГРАЦИЯ С НАПАРНИКОМ:
 * Команда читает поле {@code lastLessonStatus} из {@link UserProgress}.
 * Это поле должно обновляться напарником после выполнения команды /submit 
 * (когда его сервис оценки вернет объект LessonScore).
 * </p>
 */
public class NextCommand implements Command {

    private final BookRepository bookRepo;
    private final LessonRepository lessonRepo;
    private final UserProgressRepository progressRepo;

    public NextCommand(BookRepository bookRepo, LessonRepository lessonRepo, UserProgressRepository progressRepo) {
        this.bookRepo = bookRepo;
        this.lessonRepo = lessonRepo;
        this.progressRepo = progressRepo;
    }

    @Override
    public String getName() {
        return "next";
    }

    @Override
    public String getDescription() {
        return "Перейти к следующему уроку (если тест сдан)";
    }

    @Override
    public String getDetailedHelp() {
        return "/next — перейти к следующему уроку. Доступно только после успешного прохождения теста текущего урока (/quiz и /submit).";
    }

    @Override
    public String execute(MessageContext context) {
        long peerId = context.peerId();

        // 1. Проверяем, есть ли у пользователя прогресс
        UserProgress progress = progressRepo.findByPeerId(peerId);
        if (progress == null || progress.getBookId() == null) {
            return "⚠️ Вы ещё не начали изучение. Укажите книгу: /learn <название книги>";
        }

        if (progress.getStatus() == UserProgress.Status.DONE) {
            return "🎉 Вы уже полностью прошли эту книгу! Можете начать новую: /learn <название>.";
        }

        // 2. ПРОВЕРКА СТАТУСА ТЕСТА (Точка интеграции с напарником)
        LessonScore.Status lastStatus = progress.getLastLessonStatus();
        
        if (lastStatus == null) {
            return "⚠️ Вы ещё не прошли тест по текущему уроку. Сначала напишите /quiz, а затем /submit.";
        }

        // 3. КОРРЕКТИРОВКА ПЛАНА
        if (lastStatus == LessonScore.Status.FAILED) {
            // Тест провален — заставляем повторить урок
            progress.markAsRepeating();
            progressRepo.save(progress);
            
            return "❌ Тест не сдан (" + getPercentHint(lastStatus) + "%). Урок не засчитан.\n" +
                   "Пожалуйста, перечитайте материал и попробуйте снова.\n" +
                   "Используйте /learn, чтобы вернуться к уроку, или /quiz, чтобы пройти тест заново.";
        }

        // Если PASSED или CONDITIONALLY_PASSED — переходим дальше
        // (Напарник может добавить вывод разбора ошибок для CONDITIONALLY_PASSED, но переход разрешен)

        // 4. ПОИСК СЛЕДУЮЩЕГО УРОКА
        int nextLessonNumber = progress.getCurrentLessonNumber() + 1;
        List<Lesson> lessons = lessonRepo.findByBookId(progress.getBookId());
        
        Lesson nextLesson = lessons.stream()
                .filter(l -> l.getLessonNumber() == nextLessonNumber)
                .findFirst()
                .orElse(null);

        // 5. ОБНОВЛЕНИЕ ПРОГРЕССА
        if (nextLesson == null) {
            // Уроков больше нет — книга пройдена
            progress.markAsDone();
            progressRepo.save(progress);
            return "🎉 Поздравляем! Вы успешно завершили изучение книги!\n" +
                   "Используйте /status, чтобы посмотреть свою статистику, или /learn <новая книга>, чтобы начать новый курс.";
        }

        // Переход к следующему уроку
        progress.moveToNextLesson();
        progress.setStatus(UserProgress.Status.STUDYING);
        progress.setLastLessonStatus(null); // Сбрасываем статус, чтобы требовать новый тест
        progressRepo.save(progress);

        return formatLesson(nextLesson, progress);
    }

    /**
     * Форматирует вывод следующего урока.
     */
    private String formatLesson(Lesson lesson, UserProgress progress) {
        // Находим название книги для красивого вывода
        String bookTitle = "книги";
        for (Book b : bookRepo.findAll()) {
            if (b.getId().equals(progress.getBookId())) {
                bookTitle = "\"" + b.getTitle() + "\"";
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("✅ Переход к следующему уроку выполнен!\n\n");
        sb.append("📚 Книга: ").append(bookTitle).append("\n");
        sb.append("📖 Урок ").append(lesson.getLessonNumber())
          .append(": «").append(lesson.getTopic()).append("»\n\n");
        sb.append(lesson.getContent()).append("\n\n");
        sb.append("────────────────────\n");
        sb.append("💡 Изучите материал. Когда будете готовы, напишите /quiz");
        
        return sb.toString();
    }

    /**
     * Вспомогательный метод для красивого вывода процентов (опционально).
     */
    private String getPercentHint(LessonScore.Status status) {
        if (status == LessonScore.Status.FAILED) return "< 60";
        if (status == LessonScore.Status.CONDITIONALLY_PASSED) return "60-79";
        return ">= 80";
    }
}