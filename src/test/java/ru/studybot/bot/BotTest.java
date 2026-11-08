package ru.studybot.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.studybot.command.AboutCommand;
import ru.studybot.command.AuthorCommand;
import ru.studybot.command.CommandRegistry;
import ru.studybot.command.HelpCommand;
import ru.studybot.command.UploadCommand;
import ru.studybot.repository.impl.InMemoryBookRepository;
import ru.studybot.repository.impl.InMemoryLessonRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotTest {
    private CommandRegistry registry;
    private Bot bot;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
        registry.register(new AuthorCommand());
        registry.register(new AboutCommand());
        registry.register(new HelpCommand(registry));
        registry.register(new UploadCommand(
                new InMemoryBookRepository(), new InMemoryLessonRepository()));
        bot = new Bot(registry);
    }

    @Test
    void processesSlashCommand() {
        assertTrue(bot.processMessage("/author", 1L, List.of()).contains("Авторы"));
    }

    @Test
    void processesCommandWithoutSlash() {
        assertTrue(bot.processMessage("about", 1L, List.of()).contains("Study Bot"));
    }

    @Test
    void unknownCommandReturnsMessage() {
        assertTrue(bot.processMessage("/unknown", 1L, List.of()).contains("Неизвестная команда"));
    }

    @Test
    void emptyMessageReturnsHint() {
        assertTrue(bot.processMessage("   ", 1L, List.of()).contains("/help"));
    }

    @Test
    void helpWithArgument() {
        assertTrue(bot.processMessage("/help author", 1L, List.of()).contains("author"));
    }

    @Test
    void uploadWithAttachmentHandlesInvalidUrl() {
        // localhost:1 — соединение отклоняется мгновенно, тест не зависит от сети
        String result = bot.processMessage("/upload Книга", 1L, List.of("http://localhost:1/file.pdf"));
        assertTrue(result.contains("Не удалось"));
    }
}
