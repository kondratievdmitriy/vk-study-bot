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
    String execute(String args, long userId);
}
