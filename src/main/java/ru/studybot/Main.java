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
import ru.studybot.repository.impl.PgBookRepository;
import ru.studybot.repository.impl.PgLessonRepository;
import ru.studybot.repository.impl.PgUserProgressRepository;

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

        // Репозитории: PostgreSQL (задача 4) или in-memory (переключатель db.enabled).
        // Включённая БД при недоступном сервере даёт понятную ошибку сразу на старте —
        // это лучше, чем падение бота на первом же сообщении пользователя.
        boolean useDb = Boolean.parseBoolean(BotConfig.get("db.enabled"));
        BookRepository bookRepo;
        LessonRepository lessonRepo;
        UserProgressRepository progressRepo;
        if (useDb) {
            bookRepo = new PgBookRepository();
            lessonRepo = new PgLessonRepository();
            progressRepo = new PgUserProgressRepository();
            System.out.println("Хранилище: PostgreSQL (" + BotConfig.get("db.url") + ")");
        } else {
            bookRepo = new InMemoryBookRepository();
            lessonRepo = new InMemoryLessonRepository();
            progressRepo = new InMemoryUserProgressRepository();
            System.out.println("Хранилище: in-memory (db.enabled=false)");
        }

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
