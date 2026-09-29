package ru.studybot;

import ru.studybot.bot.Bot;
import ru.studybot.command.AboutCommand;
import ru.studybot.command.AuthorCommand;
import ru.studybot.command.CommandRegistry;
import ru.studybot.command.HelpCommand;
import ru.studybot.platform.vk.VKPlatform;

public class Main {
    public static void main(String[] args) {
        String token = System.getenv("VK_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("Не задана переменная окружения VK_TOKEN");
            System.exit(1);
        }
        CommandRegistry registry = new CommandRegistry();
        registry.register(new AuthorCommand());
        registry.register(new AboutCommand());
        registry.register(new HelpCommand(registry));
        Bot bot = new Bot(registry);
        VKPlatform platform = new VKPlatform(token, bot);
        platform.start();
    }
}
