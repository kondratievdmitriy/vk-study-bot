package ru.studybot.command;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
/**
 * Реестр команд. Хранит все зарегистрированные команды бота и предоставляет 
 * методы для их поиска, проверки наличия и получения общего списка.
 */

public class CommandRegistry {
    private final Map<String, Command> commands = new LinkedHashMap<>();

    public void register(Command command) {
        commands.put(command.getName(), command);
    }

    public Optional<Command> getCommand(String name) {
        return Optional.ofNullable(commands.get(name));
    }

    public Collection<Command> getAllCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }
}
