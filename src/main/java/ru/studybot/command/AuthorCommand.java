package ru.studybot.command;
/**
 * Команда /author.
 * Предоставляет пользователю информацию о создателях и разработчиках проекта Study Bot.
 */

public class AuthorCommand implements Command {
    @Override
    public String getName() { return "author"; }
    @Override
    public String getDescription() { return "Информация об авторах проекта"; }
    @Override
    public String getDetailedHelp() {
        return "/author — выводит информацию об авторах проекта.\nИспользование: /author";
    }
    @Override
    public String execute(String args, long userId) {
        return "📚 Study Bot\nАвторы проекта:\n"
                + "— [Дмитрий Кондратьев] — разработчик\n"
                + "— [Ашихмин Егор] — разработчик\n\n"
                + "Проект создан в рамках изучения ООП на Java.";
    }
}
