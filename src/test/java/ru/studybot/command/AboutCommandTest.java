package ru.studybot.command;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для команды AboutCommand.
 * Проверяют базовые контракты интерфейса Command и корректность 
 * возвращаемого текста описания бота.
 */
class AboutCommandTest {
    private final AboutCommand command = new AboutCommand();

    @Test
    void nameIsAbout() {
        assertEquals("about", command.getName());
    }

    @Test
    void descriptionIsNotEmpty() {
        assertFalse(command.getDescription().isBlank());
    }

    @Test
    void executeReturnsConceptDescription() {
        // ИЗМЕНЕНИЕ: создаем MessageContext вместо передачи двух параметров
        MessageContext context = new MessageContext(1L, "", List.of());
        String result = command.execute(context);
        
        assertTrue(result.contains("Study Bot"));
        assertTrue(result.contains("блоки"));
    }
}