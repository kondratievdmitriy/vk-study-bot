package ru.studybot.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для команды HelpCommand.
 * Проверяют корректность генерации общего списка команд,
 * вывод подробной справки по конкретной команде и обработку ошибок.
 */
class HelpCommandTest {
    private CommandRegistry registry;
    private HelpCommand helpCommand;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
        registry.register(new AuthorCommand());
        registry.register(new AboutCommand());
        helpCommand = new HelpCommand(registry);
        registry.register(helpCommand);
    }

    /** Без аргументов /help перечисляет все зарегистрированные команды. */
    @Test
    void noArgsListsAllCommands() {
        // ИЗМЕНЕНИЕ: создаем MessageContext
        MessageContext context = new MessageContext(1L, "", List.of());
        String result = helpCommand.execute(context);
        assertTrue(result.contains("/author"));
        assertTrue(result.contains("/about"));
        assertTrue(result.contains("/help"));
    }

    /** С корректным аргументом /help показывает подробную справку по этой команде. */
    @Test
    void withValidArgReturnsDetailedHelp() {
        MessageContext context = new MessageContext(1L, "author", List.of());
        assertTrue(helpCommand.execute(context).contains("author"));
    }

    /** С неизвестным аргументом /help честно сообщает, что команда не найдена. */
    @Test
    void withUnknownArgReturnsNotFound() {
        MessageContext context = new MessageContext(1L, "nonexistent", List.of());
        assertTrue(helpCommand.execute(context).contains("не найдена"));
    }

    /** Справка строится из реестра: новая команда появляется в /help автоматически,
     *  без правки HelpCommand — это защищает список от рассинхронизации. */
    @Test
    void newCommandAppearsInHelpAutomatically() {
        registry.register(new StubCommand("newcmd", "Новая команда для теста"));
        MessageContext context = new MessageContext(1L, "", List.of());
        assertTrue(helpCommand.execute(context).contains("/newcmd"),
                "Новая команда должна автоматически появиться в /help");
    }

    // ИЗМЕНЕНИЕ: обновлен внутренний класс StubCommand под новый интерфейс
    private static class StubCommand implements Command {
        private final String name;
        private final String desc;

        StubCommand(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getDescription() { return desc; }

        // ИЗМЕНЕНИЕ: сигнатура метода теперь принимает MessageContext
        @Override
        public String execute(MessageContext context) { return "stub"; }
    }
}
