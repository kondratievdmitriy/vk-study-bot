package ru.studybot.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.studybot.command.AboutCommand;
import ru.studybot.command.AuthorCommand;
import ru.studybot.command.CommandRegistry;
import ru.studybot.command.HelpCommand;
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
        bot = new Bot(registry);
    }

    @Test
    void processesSlashCommand() {
        assertTrue(bot.processMessage("/author", 1L).contains("Авторы"));
    }

    @Test
    void processesCommandWithoutSlash() {
        assertTrue(bot.processMessage("about", 1L).contains("Study Bot"));
    }

    @Test
    void unknownCommandReturnsMessage() {
        assertTrue(bot.processMessage("/unknown", 1L).contains("Неизвестная команда"));
    }

    @Test
    void emptyMessageReturnsHint() {
        assertTrue(bot.processMessage("   ", 1L).contains("/help"));
    }

    @Test
    void helpWithArgument() {
        assertTrue(bot.processMessage("/help author", 1L).contains("author"));
    }
}
