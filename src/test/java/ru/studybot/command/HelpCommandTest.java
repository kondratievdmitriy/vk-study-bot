package ru.studybot.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void noArgsListsAllCommands() {
        String result = helpCommand.execute("", 1L);
        assertTrue(result.contains("/author"));
        assertTrue(result.contains("/about"));
        assertTrue(result.contains("/help"));
    }

    @Test
    void withValidArgReturnsDetailedHelp() {
        assertTrue(helpCommand.execute("author", 1L).contains("author"));
    }

    @Test
    void withUnknownArgReturnsNotFound() {
        assertTrue(helpCommand.execute("nonexistent", 1L).contains("не найдена"));
    }

    @Test
    void newCommandAppearsInHelpAutomatically() {
        registry.register(new StubCommand("newcmd", "Новая команда для теста"));
        assertTrue(helpCommand.execute("", 1L).contains("/newcmd"),
                "Новая команда должна автоматически появиться в /help");
    }

    private static class StubCommand implements Command {
        private final String name;
        private final String desc;
        StubCommand(String name, String desc) { this.name = name; this.desc = desc; }
        @Override public String getName() { return name; }
        @Override public String getDescription() { return desc; }
        @Override public String execute(String args, long userId) { return "stub"; }
    }
}
