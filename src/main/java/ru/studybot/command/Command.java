package ru.studybot.command;

/**
 * Базовый интерфейс для всех команд бота.
 * <p>
 * Определяет единый контракт, которому должна следовать любая команда, 
 * чтобы её можно было корректно зарегистрировать в {@link CommandRegistry} 
 * и выполнить через главный диспетчер {@link ru.studybot.bot.Bot}.
 * </p>
 */
public interface Command {
    String getName();
    String getDescription();
    
    default String getDetailedHelp() {
        return getDescription();
    }
    
    // ИЗМЕНЕНИЕ ЗДЕСЬ: теперь метод принимает один объект MessageContext 
    // вместо двух отдельных параметров (String args, long userId)
    String execute(MessageContext context);
}