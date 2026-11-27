package ru.studybot.db;

import ru.studybot.BotConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Подключение к PostgreSQL (задача 4).
 * <p>
 * Настройки читаются из config.properties (как и все настройки бота):
 * db.url, db.user, db.password. Пароль не хранится в коде и не
 * попадает в репозиторий — config.properties в .gitignore.
 * </p>
 * <p>
 * Соединения создаются на каждый запрос (DriverManager) — для учебного
 * проекта с несколькими пользователями этого достаточно; пул соединений
 * (HikariCP) — оптимизация на будущее, если упрёмся в производительность.
 * </p>
 */
public final class Database {

    private Database() {
        // утилитный класс — конструктор не нужен
    }

    /** Новое соединение с базой. Настройки — из config.properties. */
    public static Connection getConnection() {
        String url = BotConfig.get("db.url");
        String user = BotConfig.get("db.user");
        String password = BotConfig.get("db.password");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "Не задано db.url в config.properties — база данных не настроена");
        }
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Не удалось подключиться к PostgreSQL (" + url
                            + "). Проверьте, что сервер запущен и db.user/db.password верны",
                    e);
        }
    }
}
