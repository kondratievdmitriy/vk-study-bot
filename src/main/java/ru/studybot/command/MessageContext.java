package ru.studybot.command;

import java.util.List;

/**
 * Контекст сообщения, содержащий все необходимые данные для обработки команды.
 */
public record MessageContext(
    long peerId,                // ID пользователя или беседы
    String args,                // Текст аргументов после названия команды
    List<String> attachmentUrls // Список URL-адресов прикрепленных файлов (если есть)
) {}