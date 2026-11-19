package ru.studybot.bot;

import ru.studybot.command.Command;
import ru.studybot.command.CommandRegistry;
import ru.studybot.command.MessageContext;

import java.util.List;
import java.util.Optional;

/**
 * Главный класс бота, отвечающий за первичную обработку входящих текстовых сообщений.
 * Выступает в роли диспетчера (роутера): разбирает текст сообщения, 
 * извлекает название команды и аргументы, ищет команду в реестре и передает ей управление.
 */
public class Bot {
    private static final String PREFIX = "/";
    private final CommandRegistry registry;

    public Bot(CommandRegistry registry) {
        this.registry = registry;
    }

    // ИЗМЕНЕНИЕ: добавлен параметр attachmentUrls для передачи вложений
    public String processMessage(String text, long peerId, List<String> attachmentUrls) {
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
        
        // ИЗМЕНЕНИЕ: создаем контекст и передаем его в команду
        MessageContext context = new MessageContext(peerId, args, attachmentUrls);
        return command.get().execute(context);
    }
}