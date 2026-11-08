package ru.studybot.command;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.repository.impl.InMemoryBookRepository;
import ru.studybot.repository.impl.InMemoryLessonRepository;
import ru.studybot.splitter.TextSplitter;

class UploadCommandTest {

    private final BookRepository bookRepo = new InMemoryBookRepository();
    private final LessonRepository lessonRepo = new InMemoryLessonRepository();
    private final UploadCommand command =
            new UploadCommand(bookRepo, lessonRepo, new TextSplitter());

    @Test
    void execute_successfullySavesBookAndLessons() {
        String args = "Java ООП|Основы ООП. Инкапсуляция — это сокрытие деталей реализации. "
                + "Полиморфизм позволяет переопределять методы.";
        String result = command.execute(new MessageContext(99999L, args, List.of()));

        assertTrue(result.contains("Материал успешно загружен"));
        assertEquals(1, bookRepo.findAll().size());
        assertNotNull(bookRepo.findByTitle("Java ООП"));
        assertTrue(lessonRepo.findAll().size() >= 1);
    }

    @Test
    void execute_splitsTextByChapterHeadings() {
        String args = "Учебник|"
                + "Вводный абзац о книге.\n\n"
                + "Глава 1: Основы\n"
                + "Текст первой главы с изложением материала основ.\n\n"
                + "Глава 2: Продвинутые темы\n"
                + "Текст второй главы с более сложным материалом.";
        String result = command.execute(new MessageContext(99999L, args, List.of()));

        assertTrue(result.contains("Материал успешно загружен"));
        assertEquals(2, lessonRepo.findAll().size());
        assertEquals("Глава 1: Основы", lessonRepo.findAll().get(0).getTopic());
        assertEquals(1, lessonRepo.findAll().get(0).getLessonNumber());
        assertEquals(2, lessonRepo.findAll().get(1).getLessonNumber());
    }

    @Test
    void execute_appendsLessonsToExistingBook() {
        command.execute(new MessageContext(99999L, "Книга|Глава 1: Начало. Текст первой главы.", List.of()));
        command.execute(new MessageContext(99999L, "Книга|Глава 2: Продолжение. Текст второй главы.", List.of()));

        assertEquals(1, bookRepo.findAll().size());
        assertEquals(2, lessonRepo.findAll().size());
        assertTrue(lessonRepo.findAll().get(1).getLessonNumber() == 1
                || lessonRepo.findAll().get(1).getLessonNumber() == 2);
    }

    @Test
    void execute_handlesInvalidFormat() {
        String result = command.execute(new MessageContext(99999L, "Только название без разделителя", List.of()));
        assertTrue(result.contains("Ошибка: неверный формат"));
    }

    @Test
    void execute_returnsErrorOnEmptyArgs() {
        String result = command.execute(new MessageContext(99999L, "", List.of()));
        assertTrue(result.contains("Использование"));
    }

    @Test
    void execute_withFakeAttachmentReturnsErrorMessage() {
        // localhost:1 — соединение отклоняется мгновенно, без ожидания
        MessageContext context = new MessageContext(99999L, "Книга", List.of("http://localhost:1/file.pdf"));
        String result = command.execute(context);
        assertTrue(result.contains("Не удалось"));
    }
}
