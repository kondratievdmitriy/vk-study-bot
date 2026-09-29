package ru.studybot.bot;

import ru.studybot.command.Command;
import ru.studybot.command.CommandRegistry;
import java.util.Optional;

public class Bot {
    private static final String PREFIX = "/";
    private final CommandRegistry registry;

    public Bot(CommandRegistry registry) {
        this.registry = registry;
    }

    public String processMessage(String text, long userId) {
        if (text == null || text.isBlank()) {
            return "Пустое сообщение. Введите /help, чтобы увидеть список команд.";
        }
        String normalized = text.strip();
        if (normalized.startsWith(PREFIX)) {
            normalized = normalized.substring(PREFIX.length());
        }
        int spaceIdx = normalized.indexOf(' ');
        String commandName = (spaceIdx == -1) ? normalized : normalized.substring(0, spaceIdx);
        String args = (spaceIdx == -1) ? "" : normalized.substring(spaceIdx + 1).strip();

        Optional<Command> command = registry.getCommand(commandName);
        if (command.isEmpty()) {
            return "Неизвестная команда: " + commandName + "\nВведите /help, чтобы увидеть список доступных команд.";
        }
        return command.get().execute(args, userId);
    }
}
