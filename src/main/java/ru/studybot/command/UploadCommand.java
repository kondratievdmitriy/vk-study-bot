package ru.studybot.command;

import ru.studybot.DocumentReader;
import ru.studybot.FileDownloader;
import ru.studybot.model.Book;
import ru.studybot.model.Lesson;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.splitter.TextSplitter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Команда /upload — единая точка загрузки учебного материала.
 * <p>
 * Два сценария:
 * 1. С файлом: /upload Название книги + прикреплённый файл (TXT, PDF, DOCX).
 *    Файл скачивается, читается, текст разбивается на блоки-уроки.
 * 2. С текстом: /upload Название книги | Текст материала (первая «|» — разделитель).
 *    Текст разбивается на блоки-уроки.
 * </p>
 */
public class UploadCommand implements Command {

    private final BookRepository bookRepo;
    private final LessonRepository lessonRepo;
    private final TextSplitter splitter;

    public UploadCommand(BookRepository bookRepo, LessonRepository lessonRepo) {
        this(bookRepo, lessonRepo, new TextSplitter());
    }

    public UploadCommand(BookRepository bookRepo, LessonRepository lessonRepo, TextSplitter splitter) {
        this.bookRepo = bookRepo;
        this.lessonRepo = lessonRepo;
        this.splitter = splitter;
    }

    @Override
    public String getName() {
        return "upload";
    }

    @Override
    public String getDescription() {
        return "Загрузка учебного материала (файлом или текстом)";
    }

    @Override
    public String getDetailedHelp() {
        return "/upload <Название книги> + прикреплённый файл — загрузка из файла (TXT, PDF, DOCX).\n"
                + "/upload <Название книги> | <Текст> — загрузка текстом.\n"
                + "Текст автоматически разбивается на блоки-уроки.\n"
                + "Пример: /upload Java ООП | Глава 1: Инкапсуляция — это сокрытие деталей реализации...";
    }

    @Override
    public String execute(MessageContext context) {
        List<String> attachments = context.attachmentUrls();
        if (attachments != null && !attachments.isEmpty()) {
            return uploadFromFile(attachments.get(0), context.args());
        }
        return uploadFromText(context.args());
    }

    /** Сценарий 1: материал приходит файлом. Аргументы — название книги (опционально). */
    private String uploadFromFile(String fileUrl, String args) {
        String bookTitle = (args != null && !args.isBlank()) ? args.strip() : titleFromUrl(fileUrl);
        try {
            Path tempFile = FileDownloader.downloadFile(fileUrl);
            String text = DocumentReader.readFile(tempFile.toString());
            Files.deleteIfExists(tempFile);
            return saveMaterial(bookTitle, text);
        } catch (IllegalArgumentException e) {
            return "❌ Ошибка: " + e.getMessage();
        } catch (Exception e) {
            return "❌ Не удалось прочитать файл. Убедитесь, что это TXT, PDF или DOCX.";
        }
    }

    /** Сценарий 2: материал приходит текстом в формате «Название книги | Текст». */
    private String uploadFromText(String args) {
        if (args == null || args.isBlank()) {
            return "⚠️ Использование: /upload <Название книги> | <Текст> "
                    + "или прикрепите файл (TXT, PDF, DOCX) к сообщению.";
        }
        String[] parts = args.split("\\|", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return "Ошибка: неверный формат. Используйте: Название книги | Текст (разделитель |)";
        }
        return saveMaterial(parts[0].strip(), parts[1].strip());
    }

    /** Общая логика: разбиение на уроки и сохранение в репозитории. */
        private String saveMaterial(String bookTitle, String text) {
        Book existing = bookRepo.findByTitle(bookTitle);
        Book book = existing != null ? existing : new Book(bookTitle);
        if (existing == null) {
            bookRepo.save(book);
        }

        List<Lesson> lessons = splitter.split(text, book.getId());
        if (lessons.isEmpty()) {
            return "Ошибка: не удалось извлечь текст материала.";
        }

        int totalLessons = (existing != null ? existing.getTotalLessons() : 0) + lessons.size();
        book.setTotalLessons(totalLessons);
        for (Lesson lesson : lessons) {
            lessonRepo.save(lesson);
        }

        return "Материал успешно загружен!\n"
                + "Книга: " + bookTitle + "\n"
                + "Новых уроков: " + lessons.size() + "\n"
                + "Всего уроков в книге: " + totalLessons + "\n"
                + "Номера уроков: " + lessons.get(0).getLessonNumber()
                + "–" + lessons.get(lessons.size() - 1).getLessonNumber();
    }

    /** Если пользователь не указал название книги — берём имя файла. */
    private String titleFromUrl(String fileUrl) {
        String path = fileUrl;
        int query = path.indexOf('?');
        if (query != -1) path = path.substring(0, query);
        int slash = path.lastIndexOf('/');
        String name = slash != -1 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot != -1 ? name.substring(0, dot) : name;
    }
}

