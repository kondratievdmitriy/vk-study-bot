package ru.studybot.command;

public class AboutCommand implements Command {
    @Override
    public String getName() { return "about"; }
    @Override
    public String getDescription() { return "Назначение и концепция бота"; }
    @Override
    public String getDetailedHelp() {
        return "/about — выводит описание назначения бота и его концепции.\nИспользование: /about";
    }
    @Override
    public String execute(String args, long userId) {
        return "📖 Study Bot — бот для поэтапного изучения учебных материалов.\n\n"
                + "Как это работает:\n"
                + "1. Вы загружаете учебный материал (книгу, конспект лекций).\n"
                + "2. Бот разбивает материал на блоки-уроки.\n"
                + "3. Вы изучаете блоки по расписанию и отвечаете на проверочные вопросы.\n"
                + "4. Бот оценивает усвоение и корректирует дальнейший план.\n\n"
                + "Позже бот будет поддерживать несколько пользователей с общей базой материалов.";
    }
}
