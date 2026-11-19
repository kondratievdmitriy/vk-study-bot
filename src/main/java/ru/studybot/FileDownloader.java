package ru.studybot;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

/**
 * Утилита для скачивания файлов по URL во временную папку.
 * <p>
 * Особенности:
 * - следует HTTP-редиректам (VK отдаёт ссылку на документ через 302 на CDN,
 *   без этой настройки файл скачивается пустым);
 * - расширение временного файла определяется по Content-Type финального
 *   ответа, а при отсутствии — из URL.
 * </p>
 */
public class FileDownloader {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".doc", ".txt");

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "application/pdf", ".pdf",
            "text/plain", ".txt",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx",
            "application/msword", ".doc"
    );

    public static Path downloadFile(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL) // <-- ключевой фикс
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        String ext = extensionFromContentType(
                response.headers().firstValue("Content-Type").orElse(""));
        if (ext == null) {
            ext = extensionOf(url);
        }

        Path tempFile = Files.createTempFile("vk_bot_doc_", ext);
        Files.copy(response.body(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    /** Сопоставляет Content-Type с расширением. Возвращает null, если тип неизвестен. */
    static String extensionFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String base = contentType.split(";")[0].strip().toLowerCase();
        return CONTENT_TYPE_TO_EXTENSION.get(base);
    }

    /**
     * Извлекает расширение из URL (без query-параметров).
     * Если расширения нет — считается текстовым файлом (.txt).
     * Если расширение не из списка поддерживаемых — бросает IllegalArgumentException.
     */
    public static String extensionOf(String url) {
        String path = url;
        int query = path.indexOf('?');
        if (query != -1) path = path.substring(0, query);
        int fragment = path.indexOf('#');
        if (fragment != -1) path = path.substring(0, fragment);
        int slash = path.lastIndexOf('/');
        if (slash != -1) path = path.substring(slash + 1);

        int dot = path.lastIndexOf('.');
        if (dot == -1 || dot == path.length() - 1) {
            return ".txt";
        }
        String ext = path.substring(dot).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "Неподдерживаемый формат файла. Поддерживаются: .txt, .pdf, .docx");
        }
        return ext;
    }
}

