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

    /** Имя команды — контракт реестра: пользователь набирает «/about». */
    @Test
    void nameIsAbout() {
        assertEquals("about", command.getName());
    }

    /** Описание не пустое — оно попадает в сводный список /help. */
    @Test
    void descriptionIsNotEmpty() {
        assertFalse(command.getDescription().isBlank());
    }

    /** Ответ рассказывает о боте и его ключевой идее (разбиение на блоки-уроки). */
    @Test
    void executeReturnsConceptDescription() {
        // ИЗМЕНЕНИЕ: создаем MessageContext вместо передачи двух параметров
        MessageContext context = new MessageContext(1L, "", List.of());
        String result = command.execute(context);

        assertTrue(result.contains("Study Bot"));
        assertTrue(result.contains("блоки"));
    }
}
