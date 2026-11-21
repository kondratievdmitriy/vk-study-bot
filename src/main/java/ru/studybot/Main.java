package ru.studybot;

import ru.studybot.bot.Bot;
import ru.studybot.command.AboutCommand;
import ru.studybot.command.AuthorCommand;
import ru.studybot.command.CommandRegistry;
import ru.studybot.command.HelpCommand;
import ru.studybot.command.LearnCommand;
import ru.studybot.command.NextCommand;
import ru.studybot.command.QuizCommand;
import ru.studybot.command.SubmitCommand;
import ru.studybot.command.UploadCommand;
import ru.studybot.platform.vk.VKPlatform;
import ru.studybot.repository.BookRepository;
import ru.studybot.repository.LessonRepository;
import ru.studybot.repository.UserProgressRepository;
import ru.studybot.repository.impl.InMemoryBookRepository;
import ru.studybot.repository.impl.InMemoryLessonRepository;
import ru.studybot.repository.impl.InMemoryUserProgressRepository;

/**
 * Точка входа в приложение (Entry Point).
 */
public class Main {
    public static void main(String[] args) {
        String token = BotConfig.get("vk.access.token");
        int groupId = Integer.parseInt(BotConfig.get("vk.group.id"));

        if (token == null || token.isBlank()) {
            System.err.println("Не задана переменная окружения VK_TOKEN");
            System.exit(1);
        }

        CommandRegistry registry = new CommandRegistry();

        // Репозитории (InMemory — заглушка до БД в задаче 4)
        BookRepository bookRepo = new InMemoryBookRepository();
        LessonRepository lessonRepo = new InMemoryLessonRepository();
        UserProgressRepository progressRepo = new InMemoryUserProgressRepository();

        // Регистрируем команды
        registry.register(new AuthorCommand());
        registry.register(new AboutCommand());
        registry.register(new HelpCommand(registry));
        registry.register(new UploadCommand(bookRepo, lessonRepo));
        registry.register(new LearnCommand(bookRepo, lessonRepo, progressRepo));
        registry.register(new QuizCommand(bookRepo, lessonRepo));
        registry.register(new SubmitCommand(bookRepo, lessonRepo, progressRepo));
        registry.register(new NextCommand(bookRepo, lessonRepo, progressRepo));

        Bot bot = new Bot(registry);
        VKPlatform platform = new VKPlatform(token, groupId, bot);
        platform.start();
    }
}
