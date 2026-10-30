package ru.studybot.command;

import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.model.Book;
import ru.studybot.model.Lesson;

public class UploadCommand implements Command {
    private final BookRepository bookRepo;
    private final LessonRepository lessonRepo;

    public UploadCommand(BookRepository bookRepo, LessonRepository lessonRepo) {
        this.bookRepo = bookRepo;
        this.lessonRepo = lessonRepo;
    }

    @Override
    public String getName() {
        return "/upload";
    }

    @Override
    public String getDescription() {
        return "Загрузка учебного материала: название книги|тема|текст";
    }

    @Override
    public String execute(String args, long userId) {
        if (args == null || args.trim().isEmpty()) {
            return "Ошибка: укажите данные в формате: Название книги|Тема|Текст";
        }

        String[] parts = args.split("\\|", 3);
        if (parts.length < 3) {
            return "Ошибка: неверный формат. Используйте: Название книги|Тема|Текст (разделитель |)";
        }

        String bookTitle = parts[0].trim();
        String lessonTopic = parts[1].trim();
        String content = parts[2].trim();

        Book existingBook = bookRepo.findByTitle(bookTitle);
        String bookId;

        if (existingBook != null) {
            bookId = existingBook.getId();
        } else {
            Book newBook = new Book(bookTitle);
            bookRepo.save(newBook);
            bookId = newBook.getId();
        }

        Lesson lesson = new Lesson(bookId, lessonTopic, content);
        lessonRepo.save(lesson);

        int previewLen = Math.min(100, content.length());
        String preview = content.substring(0, previewLen) + (content.length() > previewLen ? "..." : "");

        return "Материал успешно загружен!\nКнига: " + bookTitle + "\nУрок: " + lessonTopic + "\nСодержимое: " + preview;
    }
}
