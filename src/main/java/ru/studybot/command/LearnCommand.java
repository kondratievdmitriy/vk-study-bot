package ru.studybot.command;

import ru.studybot.model.Book;
import ru.studybot.model.Lesson;
import ru.studybot.model.UserProgress;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.repository.UserProgressRepository;

import java.util.List;

/**
 * Команда /learn — выдача текущего блока-урока пользователю.
 * <p>
 * Реализует Задачу 3, пункт 1: "Реализовать выдачу пользователю текущего блока-урока".
 * Управляет стартом новой книги и продолжением изучения текущей.
 * </p>
 */
public class LearnCommand implements Command {

    private final BookRepository bookRepo;
    private final LessonRepository lessonRepo;
    private final UserProgressRepository progressRepo;

    public LearnCommand(BookRepository bookRepo, LessonRepository lessonRepo, UserProgressRepository progressRepo) {
        this.bookRepo = bookRepo;
        this.lessonRepo = lessonRepo;
        this.progressRepo = progressRepo;
    }

    @Override
    public String getName() {
        return "learn";
    }

    @Override
    public String getDescription() {
        return "Начать или продолжить изучение книги";
    }

    @Override
    public String getDetailedHelp() {
        return "/learn <название книги> — начать изучение новой книги с первого урока.\n" +
               "/learn — продолжить изучение текущего урока.";
    }

    @Override
    public String execute(MessageContext context) {
        long peerId = context.peerId();
        String bookTitle = context.args();

        // 1. Получаем или создаем прогресс пользователя
        UserProgress progress = progressRepo.findByPeerId(peerId);
        if (progress == null) {
            progress = new UserProgress(peerId);
        }

        // 2. Если передано название книги — начинаем изучение с нуля
        if (bookTitle != null && !bookTitle.isBlank()) {
            Book book = bookRepo.findByTitle(bookTitle);
            if (book == null) {
                return "❌ Книга \"" + bookTitle + "\" не найдена. Проверьте название.";
            }
            
            // Инициализируем прогресс для новой книги
            progress.setBookId(book.getId());
            progress.setCurrentLessonNumber(1);
            progress.setStatus(UserProgress.Status.STUDYING);
            progressRepo.save(progress);
            
            return formatLesson(progress, book.getTitle());
        }

        // 3. Если аргументов нет — продолжаем текущее изучение
        if (progress.getBookId() == null) {
            return "⚠️ Вы ещё не начали изучение. Укажите книгу: /learn <название книги>";
        }

        if (progress.getStatus() == UserProgress.Status.DONE) {
            return "🎉 Вы уже полностью прошли эту книгу! Можете начать новую: /learn <название>.";
        }

        // Находим название книги для красивого вывода (опционально, можно просто выдать урок)
        String currentBookTitle = "текущей книги"; 
        for (Book b : bookRepo.findAll()) {
            if (b.getId().equals(progress.getBookId())) {
                currentBookTitle = b.getTitle();
                break;
            }
        }

        return formatLesson(progress, currentBookTitle);
    }

    /**
     * Форматирует вывод урока для пользователя.
     */
    private String formatLesson(UserProgress progress, String bookTitle) {
        // Ищем текущий урок в репозитории
        List<Lesson> lessons = lessonRepo.findByBookId(progress.getBookId());
        Lesson currentLesson = lessons.stream()
                .filter(l -> l.getLessonNumber() == progress.getCurrentLessonNumber())
                .findFirst()
                .orElse(null);

        if (currentLesson == null) {
            // Если урок не найден (например, удален или ошибка в данных)
            progress.markAsDone();
            progressRepo.save(progress);
            return "🎉 Поздравляем! Вы успешно завершили изучение книги \"" + bookTitle + "\".";
        }

        // Формируем красивое сообщение
        StringBuilder sb = new StringBuilder();
        sb.append("📚 Книга: ").append(bookTitle).append("\n");
        sb.append("📖 Урок ").append(currentLesson.getLessonNumber())
          .append(": «").append(currentLesson.getTopic()).append("»\n\n");
        sb.append(currentLesson.getContent()).append("\n\n");
        sb.append("────────────────────\n");
        sb.append("💡 Изучите материал. Когда будете готовы проверить знания, напишите: /quiz");
        
        return sb.toString();
    }
}