package ru.studybot.repository.impl;

import ru.studybot.model.UserProgress;
import ru.studybot.repository.UserProgressRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory реализация {@link UserProgressRepository}.
 * <p>
 * Хранит прогресс в оперативной памяти (HashMap).
 * Данные теряются при перезапуске бота.
 * </p>
 */
public class InMemoryUserProgressRepository implements UserProgressRepository {

    // Ключ — peerId пользователя, Значение — его прогресс.
    // HashMap выбран для мгновенного поиска O(1) по ID.
    private final Map<Long, UserProgress> progressMap = new HashMap<>();

    @Override
    public UserProgress findByPeerId(long peerId) {
        return progressMap.get(peerId);
    }

    @Override
    public void save(UserProgress progress) {
        // Map.put автоматически делает upsert: 
        // если ключа нет — добавляет, если есть — перезаписывает старое значение.
        progressMap.put(progress.getPeerId(), progress);
    }

    @Override
    public List<UserProgress> findAll() {
        // Возвращаем копию значений, чтобы никто случайно не сломал внутреннее хранилище
        return new ArrayList<>(progressMap.values());
    }
}