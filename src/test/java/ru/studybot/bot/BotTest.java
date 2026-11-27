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

/**
 * Дымовые тесты диспетчеризации: Bot получает сообщение и маршрутизирует его
 * в зарегистрированную команду. Проверяются синтаксис со слешем и без,
 * неизвестные команды, пустой ввод и справка по отдельной команде.
 * Команды регистрируются поверх in-memory репозиториев — база не нужна.
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
        registry.register(new UploadCommand(
                new InMemoryBookRepository(), new InMemoryLessonRepository()));
        bot = new Bot(registry);
    }

    /** «/author» распознаётся как команда и возвращает ответ со словом «Авторы». */
    @Test
    void processesSlashCommand() {
        assertTrue(bot.processMessage("/author", 1L, List.of()).contains("Авторы"));
    }

    /** Тот же маршрут работает и без слеша: «about» эквивалентно «/about». */
    @Test
    void processesCommandWithoutSlash() {
        assertTrue(bot.processMessage("about", 1L, List.of()).contains("Study Bot"));
    }

    /** Неизвестная команда не ломает бота — возвращает понятное сообщение-заглушку. */
    @Test
    void unknownCommandReturnsMessage() {
        assertTrue(bot.processMessage("/unknown", 1L, List.of()).contains("Неизвестная команда"));
    }

    /** Пустое или пробельное сообщение не падает, а подсказывает /help. */
    @Test
    void emptyMessageReturnsHint() {
        assertTrue(bot.processMessage("   ", 1L, List.of()).contains("/help"));
    }

    /** «/help <команда>» выдаёт подробную справку именно по названной команде. */
    @Test
    void helpWithArgument() {
        assertTrue(bot.processMessage("/help author", 1L, List.of()).contains("author"));
    }

    /** Вложение обрабатывается: недоступный URL даёт ошибку чтения, а не исключение.
     *  localhost:1 — соединение отклоняется мгновенно, тест не зависит от сети. */
    @Test
    void uploadWithAttachmentHandlesInvalidUrl() {
        String result = bot.processMessage("/upload Книга", 1L, List.of("http://localhost:1/file.pdf"));
        assertTrue(result.contains("Не удалось"));
    }
}

