package ru.studybot.command;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.repository.impl.InMemoryBookRepository;
import ru.studybot.repository.impl.InMemoryLessonRepository;

class UploadCommandTest {

    @Test
    void execute_successfullySavesBookAndLesson() {
        BookRepository bookRepo = new InMemoryBookRepository();
        LessonRepository lessonRepo = new InMemoryLessonRepository();
        UploadCommand command = new UploadCommand(bookRepo, lessonRepo);

        String args = "Java ООП|Глава 1: Основы|В этой главе мы изучаем основы объектно-ориентированного программирования и принципы инкапсуляции.";
        String result = command.execute(args, 99999L);

        assertTrue(result.contains("Материал успешно загружен"));
        assertEquals(1, bookRepo.findAll().size());
        assertNotNull(bookRepo.findByTitle("Java ООП"));
        assertEquals(1, lessonRepo.findAll().size());
    }

    @Test
    void execute_handlesInvalidFormat() {
        BookRepository bookRepo = new InMemoryBookRepository();
        LessonRepository lessonRepo = new InMemoryLessonRepository();
        UploadCommand command = new UploadCommand(bookRepo, lessonRepo);

        String result = command.execute("Только название", 99999L);
        assertTrue(result.contains("Ошибка: неверный формат"));
    }

    @Test
    void execute_doesNotCreateDuplicateBooks() {
        BookRepository bookRepo = new InMemoryBookRepository();
        LessonRepository lessonRepo = new InMemoryLessonRepository();
        UploadCommand command = new UploadCommand(bookRepo, lessonRepo);

        command.execute("Java ООП|Глава 1|Текст 1", 99999L);
        command.execute("Java ООП|Глава 2|Текст 2", 99999L);

        assertEquals(1, bookRepo.findAll().size());
        assertEquals(2, lessonRepo.findAll().size());
    }

    @Test
    void execute_returnsErrorOnEmptyArgs() {
        BookRepository bookRepo = new InMemoryBookRepository();
        LessonRepository lessonRepo = new InMemoryLessonRepository();
        UploadCommand command = new UploadCommand(bookRepo, lessonRepo);

        String result = command.execute("", 99999L);
        assertTrue(result.contains("Ошибка: укажите данные"));
    }
}

