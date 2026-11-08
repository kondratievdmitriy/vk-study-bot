package ru.studybot.command;

import org.junit.jupiter.api.Test;
import java.util.List;

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
        
        // ИЗМЕНЕНИЕ: создаем MessageContext
        MessageContext context = new MessageContext(99999L, args, List.of());
        String result = command.execute(context);

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

        // ИЗМЕНЕНИЕ: создаем MessageContext
        MessageContext context = new MessageContext(99999L, "Только название", List.of());
        String result = command.execute(context);
        assertTrue(result.contains("Ошибка: неверный формат"));
    }

    @Test
    void execute_doesNotCreateDuplicateBooks() {
        BookRepository bookRepo = new InMemoryBookRepository();
        LessonRepository lessonRepo = new InMemoryLessonRepository();
        UploadCommand command = new UploadCommand(bookRepo, lessonRepo);

        // ИЗМЕНЕНИЕ: создаем MessageContext для каждого вызова
        MessageContext context1 = new MessageContext(99999L, "Java ООП|Глава 1|Текст 1", List.of());
        command.execute(context1);
        
        MessageContext context2 = new MessageContext(99999L, "Java ООП|Глава 2|Текст 2", List.of());
        command.execute(context2);

        assertEquals(1, bookRepo.findAll().size());
        assertEquals(2, lessonRepo.findAll().size());
    }

    @Test
    void execute_returnsErrorOnEmptyArgs() {
        BookRepository bookRepo = new InMemoryBookRepository();
        LessonRepository lessonRepo = new InMemoryLessonRepository();
        UploadCommand command = new UploadCommand(bookRepo, lessonRepo);

        // ИЗМЕНЕНИЕ: создаем MessageContext
        MessageContext context = new MessageContext(99999L, "", List.of());
        String result = command.execute(context);
        assertTrue(result.contains("Ошибка: укажите данные"));
    }
}