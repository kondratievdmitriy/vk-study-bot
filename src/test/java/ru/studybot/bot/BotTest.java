package ru.studybot.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.studybot.command.AboutCommand;
import ru.studybot.command.AuthorCommand;
import ru.studybot.command.CommandRegistry;
import ru.studybot.command.HelpCommand;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для класса Bot.
 * Проверяют корректность парсинга сообщений, маршрутизации команд 
 * и обработки граничных случаев (edge cases) без необходимости запуска реального VK API.
 */
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
        // Добавлен третий параметр: пустой список вложений
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
    
    // Бонусный тест: проверяем, что бот корректно передает вложения (если захочешь протестировать ReadCommand позже)
    @Test
    void processesMessageWithAttachment() {
        // Здесь можно будет добавить тест для ReadCommand, когда он будет зарегистрирован
        String result = bot.processMessage("/read", 1L, List.of("http://example.com/file.pdf"));
        // Ожидаем ошибку, так как файл по этой ссылке не настоящий, но это подтверждает, что бот попытался его обработать
        assertTrue(result.contains("Не удалось прочитать файл") || result.contains("Использование"));
    }
}