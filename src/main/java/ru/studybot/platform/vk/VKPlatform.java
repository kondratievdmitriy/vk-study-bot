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
    private final Bot bot;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final Random random = new Random();

    private String lpServer;
    private String lpKey;
    private int lpTs;

    public VKPlatform(String token, Bot bot) {
        this.token = token;
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
    public void sendMessage(long userId, String text) {
        try {
            String url = API_BASE + "messages.send"
                    + "?user_id=" + userId
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
            LOG.log(Level.WARNING, "Не удалось отправить сообщение пользователю " + userId, e);
        }
    }

    private void initLongPollServer() {
        try {
            String url = API_BASE + "messages.getLongPollServer"
                    + "?access_token=" + token + "&v=" + API_VERSION;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            JsonNode resp = root.path("response");
            lpServer = resp.path("server").asText();
            lpKey = resp.path("key").asText();
            lpTs = resp.path("ts").asInt();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось получить Long Poll сервер", e);
        }
    }

    private void pollEvents() throws Exception {
        String url = "https://" + lpServer
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
        if (root.has("ts")) {
            lpTs = root.get("ts").asInt();
        }
        if (root.has("failed")) {
            initLongPollServer();
            return;
        }
        JsonNode updates = root.path("updates");
        for (JsonNode update : updates) {
            int type = update.path("type").asInt();
            if (type == 4) {
                JsonNode msg = update.path("object");
                long userId = msg.path("user_id").asLong();
                String text = msg.path("text").asText();
                int flags = update.path("extra_values").path("flags").asInt(0);
                if ((flags & 2) != 0) {
                    continue;
                }
                String reply = bot.processMessage(text, userId);
                sendMessage(userId, reply);
            }
        }
    }
}
