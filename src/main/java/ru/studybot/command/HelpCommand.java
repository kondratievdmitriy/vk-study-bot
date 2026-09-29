package ru.studybot.command;

import java.util.Collection;

public class HelpCommand implements Command {
    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }
    @Override
    public String getName() { return "help"; }
    @Override
    public String getDescription() { return "Список доступных команд и справка по конкретной команде"; }
    @Override
    public String getDetailedHelp() {
        return "/help — список всех доступных команд.\n"
                + "/help <command> — справка по конкретной команде.\n"
                + "Пример: /help author";
    }
    @Override
    public String execute(String args, long userId) {
        if (args == null || args.isBlank()) {
            return formatAllCommands();
        }
        return formatCommandHelp(args.strip());
    }
    private String formatAllCommands() {
        Collection<Command> commands = registry.getAllCommands();
        if (commands.isEmpty()) {
            return "Команды пока не зарегистрированы.";
        }
        StringBuilder sb = new StringBuilder("Доступные команды:\n\n");
        for (Command cmd : commands) {
            sb.append("  /").append(cmd.getName()).append(" — ")
              .append(cmd.getDescription()).append("\n");
        }
        sb.append("\nДля подробной справки введите /help <команда>.");
        return sb.toString();
    }
    private String formatCommandHelp(String commandName) {
        return registry.getCommand(commandName)
                .map(Command::getDetailedHelp)
                .orElseGet(() -> "Команда «" + commandName + "» не найдена. "
                        + "Введите /help, чтобы увидеть список команд.");
    }
}
