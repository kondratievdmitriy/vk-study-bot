package ru.studybot.platform;

public interface Platform {
    void start();
    void sendMessage(long userId, String text);
}
