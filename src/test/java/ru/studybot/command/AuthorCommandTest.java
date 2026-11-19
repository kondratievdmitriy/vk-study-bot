package ru.studybot.command;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для команды AuthorCommand.
 * Проверяют базовые контракты интерфейса Command и корректность 
 * возвращаемого текста с информацией об авторах.
 */
class AuthorCommandTest {
    private final AuthorCommand command = new AuthorCommand();

    @Test
    void nameIsAuthor() {
        assertEquals("author", command.getName());
    }

    @Test
    void descriptionIsNotEmpty() {
        assertFalse(command.getDescription().isBlank());
    }

    @Test
    void executeReturnsTextWithAuthors() {
        // ИЗМЕНЕНИЕ: создаем MessageContext вместо передачи двух параметров
        MessageContext context = new MessageContext(1L, "", List.of());
        assertTrue(command.execute(context).contains("Авторы"));
    }
}