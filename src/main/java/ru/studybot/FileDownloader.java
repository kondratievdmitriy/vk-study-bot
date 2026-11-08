package ru.studybot;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

/**
 * Утилита для скачивания файлов по URL во временную папку.
 * Расширение временного файла выводится из URL — иначе DocumentReader
 * не сможет определить формат (раньше все файлы получали .tmp и отбрасывались).
 */
public class FileDownloader {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".docx", ".doc", ".txt");

    public static Path downloadFile(String url) throws Exception {
        Path tempFile = Files.createTempFile("vk_bot_doc_", extensionOf(url));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        Files.copy(response.body(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    /**
     * Извлекает расширение из URL (без query-параметров).
     * Пример: "https://vk.com/doc1_2.pdf?hash=xyz" -> ".pdf".
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
