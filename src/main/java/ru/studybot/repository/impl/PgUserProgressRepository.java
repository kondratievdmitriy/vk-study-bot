package ru.studybot.repository.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.studybot.db.Database;
import ru.studybot.model.LessonScore;
import ru.studybot.model.UserProgress;
import ru.studybot.repository.UserProgressRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL-реализация хранилища прогресса пользователей (задача 4).
 * <p>
 * Структура columns по схеме БД: vk_id — идентификатор пользователя,
 * current_book_id/current_lesson — текущая позиция, progress — JSONB,
 * в котором хранятся статус изучения, статус последнего теста
 * и счётчик попыток по урокам: {"status": "STUDYING",
 * "lastLessonStatus": "PASSED", "attempts": {"1": 2}}.
 * Поле schedule_settings остаётся за задачей 5 — этот репозиторий его не трогает.
 * </p>
 */
public class PgUserProgressRepository implements UserProgressRepository {

    private static final String FIND_BY_PEER_SQL = """
            SELECT vk_id, current_book_id, current_lesson, progress
            FROM users WHERE vk_id = ?
            """;

    private static final String SAVE_SQL = """
            INSERT INTO users (vk_id, current_book_id, current_lesson, progress)
            VALUES (?, ?, ?, ?::jsonb)
            ON CONFLICT (vk_id) DO UPDATE
                SET current_book_id = EXCLUDED.current_book_id,
                    current_lesson = EXCLUDED.current_lesson,
                    progress = EXCLUDED.progress
            """;

    private static final String FIND_ALL_SQL = """
            SELECT vk_id, current_book_id, current_lesson, progress
            FROM users ORDER BY user_id
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public UserProgress findByPeerId(long peerId) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_PEER_SQL)) {
            ps.setLong(1, peerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось найти прогресс пользователя", e);
        }
    }

    @Override
    public void save(UserProgress progress) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SAVE_SQL)) {
            ps.setLong(1, progress.getPeerId());
            ps.setString(2, progress.getBookId());
            ps.setInt(3, progress.getCurrentLessonNumber());
            ps.setString(4, serializeProgress(progress));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить прогресс пользователя", e);
        }
    }

    @Override
    public List<UserProgress> findAll() {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            List<UserProgress> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать прогресс пользователей", e);
        }
    }

    // --- Сериализация/десериализация JSONB progress ---

    private String serializeProgress(UserProgress progress) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("status", progress.getStatus().name());
            if (progress.getLastLessonStatus() != null) {
                node.put("lastLessonStatus", progress.getLastLessonStatus().name());
            }
            ObjectNode attempts = node.putObject("attempts");
            for (Map.Entry<Integer, Integer> e : progress.getAttemptsPerLesson().entrySet()) {
                attempts.put(String.valueOf(e.getKey()), e.getValue());
            }
            return mapper.writeValueAsString(node);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать прогресс", e);
        }
    }

    private UserProgress mapRow(ResultSet rs) throws SQLException {
        UserProgress progress = new UserProgress(rs.getLong("vk_id"));
        progress.setBookId(rs.getString("current_book_id"));
        progress.setCurrentLessonNumber(rs.getInt("current_lesson"));

        String json = rs.getString("progress");
        if (json != null && !json.isBlank()) {
            try {
                JsonNode node = mapper.readTree(json);
                if (node.hasNonNull("status")) {
                    progress.setStatus(UserProgress.Status.valueOf(node.get("status").asText()));
                }
                if (node.hasNonNull("lastLessonStatus")) {
                    progress.setLastLessonStatus(
                            LessonScore.Status.valueOf(node.get("lastLessonStatus").asText()));
                }
                JsonNode attempts = node.get("attempts");
                if (attempts != null && attempts.isObject()) {
                    attempts.fields().forEachRemaining(e ->
                            progress.getAttemptsPerLesson()
                                    .put(Integer.parseInt(e.getKey()), e.getValue().asInt()));
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalStateException("Повреждённый JSON прогресса пользователя", e);
            }
        }
        return progress;
    }
}
