package ru.studybot.command;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(command.execute("", 1L).contains("Авторы"));
    }
}
