package ru.studybot.command;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        String result = command.execute("", 1L);
        assertTrue(result.contains("Study Bot"));
        assertTrue(result.contains("блоки"));
    }
}
