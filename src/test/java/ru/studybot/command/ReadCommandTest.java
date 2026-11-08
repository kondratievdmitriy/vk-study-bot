package ru.studybot.command;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для команды ReadCommand.
 * Проверяют обработку текста, отсутствие аргументов и корректную обработку 
 * ошибок при попытке скачать несуществующий файл.
 */
class ReadCommandTest {
    
    private final ReadCommand command = new ReadCommand();

    @Test
    void nameIsRead() {
        assertEquals("read", command.getName());
    }

    @Test
    void descriptionIsNotEmpty() {
        assertFalse(command.getDescription().isBlank());
        assertTrue(command.getDescription().contains("TXT, PDF, DOCX"));
    }

    @Test
    void executeWithTextOnlyReturnsEcho() {
        // Сценарий: пользователь написал /read привет мир (без файлов)
        MessageContext context = new MessageContext(1L, "привет мир", List.of());
        String result = command.execute(context);
        
        assertTrue(result.contains("Вы написали: привет мир"));
    }

    @Test
    void executeWithNoArgsAndNoAttachmentsReturnsHint() {
        // Сценарий: пользователь написал просто /read
        MessageContext context = new MessageContext(1L, "", List.of());
        String result = command.execute(context);
        
        assertTrue(result.contains("Использование"));
        assertTrue(result.contains("TXT, PDF, DOCX"));
    }

    @Test
    void executeWithFakeAttachmentUrlReturnsErrorMessage() {
        // Сценарий: пользователь прикрепил файл, но ссылка нерабочая.
        // Мы передаем заведомо ложный URL, чтобы проверить, что команда 
        // корректно ловит исключение и не "падает", а возвращает понятную ошибку.
        List<String> fakeAttachments = List.of("http://this-is-a-fake-url-for-testing.com/file.pdf");
        MessageContext context = new MessageContext(1L, "", fakeAttachments);
        
        String result = command.execute(context);
        
        // Ожидаем, что бот поймает ошибку сети/чтения и вернет наше сообщение об ошибке
        assertTrue(result.contains("Не удалось прочитать файл"));
    }
}