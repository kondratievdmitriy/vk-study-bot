package ru.studybot.platform.vk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.studybot.bot.Bot;
import ru.studybot.platform.Platform;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Реализация платформы VK для бота.
 * <p>
 * Отвечает за связь с VK API: получение Long Poll сервера, приём событий
 * (в том числе вложений-документов), отправка сообщений. Реализует интерфейс
 * {@link ru.studybot.platform.Platform}, поэтому ядро бота не зависит от
 * деталей протокола VK и в будущем может работать с другой платформой.
 * </p>
 * <p>
 * Сообщения отправляются POST-запросом: длинный текст урока в GET-параметре
 * раздувает URL до десятков килобайт, и сервер VK разрывает соединение
 * (EOF reached while reading). Дополнительно текст нарезается на куски
 * в пределах лимита VK на длину сообщения (4096 символов).
 * </p>
 */
public class VKPlatform implements Platform {
    private static final Logger LOG = Logger.getLogger(VKPlatform.class.getName());
    private static final String API_BASE = "https://api.vk.com/method/";
    private static final String API_VERSION = "5.199";
    private static final int LONG_POLL_WAIT = 25;

    /** Лимит VK на длину одного сообщения — с запасом ниже официальных 4096. */
    private static final int MESSAGE_CHUNK_LIMIT = 3800;

    private final String token;
    private final int groupId;
    private final Bot bot;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final Random random = new Random();

    private String lpServer;
    private String lpKey;
    private String lpTs;

    public VKPlatform(String token, int groupId, Bot bot) {
        this.token = token;
        this.groupId = groupId;
        this.bot = bot;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public void start() {
        initLongPollServer();
        LOG.info("VK Long Poll запущен. Ожидание сообщений...");
        while (true) {
            try {
                pollEvents();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Ошибка при опросе Long Poll, переподключение...", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                initLongPollServer();
            }
        }
    }

    @Override
    public void sendMessage(long peerId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String chunk : splitMessage(text)) {
            sendChunk(peerId, chunk);
        }
    }

    private void sendChunk(long peerId, String chunk) {
        try {
            // POST с телом в формате form-urlencoded: длинный текст не раздувает URL
            String body = "peer_id=" + peerId
                    + "&random_id=" + random.nextInt()
                    + "&message=" + URLEncoder.encode(chunk, StandardCharsets.UTF_8)
                    + "&access_token=" + token
                    + "&v=" + API_VERSION;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "messages.send"))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());
            // Логируем тело ответа: там VK пишет код ошибки (например, 9 — сообщение слишком длинное).
            // Раньше ответ игнорировался, и любые ошибки API выглядели как тишина.
            String body2 = response.body();
            if (body2 != null && body2.contains("\"error\"")) {
                LOG.warning("VK API вернул ошибку: " + body2);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Не удалось отправить сообщение: " + peerId, e);
        }
    }

    /** Нарезает текст на куски в пределах лимита VK, стараясь резать по строкам. */
    static List<String> splitMessage(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MESSAGE_CHUNK_LIMIT, text.length());
            if (end < text.length()) {
                int lineBreak = text.lastIndexOf('\n', end);
                if (lineBreak > start) {
                    end = lineBreak; // режем по границе строки, не посреди слова
                }
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }

    private void initLongPollServer() {
        try {
            String url = API_BASE + "groups.getLongPollServer"
                    + "?group_id=" + groupId
                    + "&access_token=" + token
                    + "&v=" + API_VERSION;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            JsonNode resp = root.path("response");
            lpServer = resp.path("server").asText();
            lpKey = resp.path("key").asText();
            lpTs = resp.path("ts").asText();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось получить Long Poll сервер", e);
        }
    }

    private void pollEvents() throws Exception {
        String url = lpServer
                + "?act=a_check"
                + "&key=" + URLEncoder.encode(lpKey, StandardCharsets.UTF_8)
                + "&ts=" + lpTs
                + "&wait=" + LONG_POLL_WAIT;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(LONG_POLL_WAIT + 5))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());

        if (root.has("failed")) {
            int failed = root.path("failed").asInt();
            LOG.warning("Long Poll ошибка: code=" + failed);
            if (failed == 1) {
                lpTs = root.path("ts").asText();
            } else {
                initLongPollServer();
            }
            return;
        }

        if (root.has("ts")) {
            lpTs = root.get("ts").asText();
        }

        JsonNode updates = root.path("updates");
        for (JsonNode update : updates) {
            String type = update.path("type").asText();
            if ("message_new".equals(type)) {
                JsonNode message = update.path("object").path("message");
                long peerId = message.path("peer_id").asLong();
                String text = message.path("text").asText();

                // Извлекаем вложения (файлы/документы) из сообщения
                List<String> attachmentUrls = new ArrayList<>();
                JsonNode attachments = message.path("attachments");
                if (attachments.isArray()) {
                    for (JsonNode att : attachments) {
                        // Ищем именно документы (doc), так как они содержат прямую ссылку на скачивание
                        if ("doc".equals(att.path("type").asText())) {
                            String urlDoc = att.path("doc").path("url").asText();
                            if (!urlDoc.isEmpty()) {
                                attachmentUrls.add(urlDoc);
                            }
                        }
                    }
                }

                LOG.info("Получено сообщение от " + peerId + ": " + text);
                String reply = bot.processMessage(text, peerId, attachmentUrls);
                sendMessage(peerId, reply);
            }
        }
    }
}
