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
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VKPlatform implements Platform {
    private static final Logger LOG = Logger.getLogger(VKPlatform.class.getName());
    private static final String API_BASE = "https://api.vk.com/method/";
    private static final String API_VERSION = "5.199";
    private static final int LONG_POLL_WAIT = 25;

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
        try {
            String url = API_BASE + "messages.send"
                    + "?peer_id=" + peerId
                    + "&random_id=" + random.nextInt()
                    + "&message=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&access_token=" + token
                    + "&v=" + API_VERSION;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Не удалось отправить сообщение: " + peerId, e);
        }
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

                LOG.info("Получено сообщение от " + peerId + ": " + text);

                String reply = bot.processMessage(text, peerId);
                sendMessage(peerId, reply);
            }
        }
    }
}

