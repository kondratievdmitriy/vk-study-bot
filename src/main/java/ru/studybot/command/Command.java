package ru.studybot.command;

public interface Command {
    String getName();
    String getDescription();
    default String getDetailedHelp() {
        return getDescription();
    }
    String execute(String args, long userId);
}
