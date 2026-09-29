package ru.studybot.command;

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
                + "— [Имя Автора 1] — разработчик\n"
                + "— [Имя Автора 2] — разработчик\n\n"
                + "Проект создан в рамках изучения ООП на Java.";
    }
}
