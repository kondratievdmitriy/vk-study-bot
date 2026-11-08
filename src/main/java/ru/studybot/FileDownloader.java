package ru.studybot;

// Импорты для работы с сетью (HTTP запросы)
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Импорты для работы с файловой системой
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Утилита для скачивания файлов из интернета по URL.
 * <p>
 * Этот класс нужен, чтобы скачать файл, который пользователь прикрепил
 * к сообщению в ВК. ВК даёт временную ссылку на файл, и мы должны
 * скачать его к себе на сервер, чтобы потом прочитать.
 * </p>
 * <p>
 * Файл скачивается во временную папку системы (обычно это C:\Users\Имя\AppData\Local\Temp
 * на Windows или /tmp на Linux/Mac). После чтения файл удаляется, чтобы не засорять диск.
 * </p>
 */
public class FileDownloader {
    
    /**
     * Скачивает файл по указанному URL во временную папку и возвращает путь к нему.
     * <p>
     * Как это работает:
     * 1. Создаём временный файл с уникальным именем
     * 2. Делаем HTTP GET запрос по ссылке
     * 3. Получаем поток данных (InputStream) из ответа
     * 4. Копируем этот поток в наш временный файл
     * 5. Возвращаем путь к файлу, чтобы его можно было прочитать
     * </p>
     *
     * @param url прямая ссылка на файл (например, https://vk.com/doc123_456?dl=1)
     * @return путь к скачанному временному файлу
     * @throws Exception если не удалось скачать файл (проблемы с сетью, битая ссылка и т.д.)
     */
    public static Path downloadFile(String url) throws Exception {
        // Создаём временный файл с префиксом "vk_bot_doc_" и расширением .tmp
        // Files.createTempFile() генерирует уникальное имя, чтобы файлы не конфликтовали
        // Например: "vk_bot_doc_123456789.tmp"
        Path tempFile = Files.createTempFile("vk_bot_doc_", ".tmp");
        
        // Создаём HTTP клиент — это как браузер, но для кода
        // HttpClient.newHttpClient() создаёт клиент со стандартными настройками
        HttpClient client = HttpClient.newHttpClient();
        
        // Создаём HTTP запрос
        // HttpRequest.newBuilder() — начинаем строить запрос
        // .uri(URI.create(url)) — указываем адрес (URL)
        // .GET() — говорим, что это GET запрос (мы хотим получить данные)
        // .build() — собираем запрос в готовый объект
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
                
        // Отправляем запрос и получаем ответ
        // client.send() — отправляет запрос и ждёт ответ
        // HttpResponse.BodyHandlers.ofInputStream() — говорит, что мы хотим
        // получить ответ как поток байт (InputStream), а не как строку
        HttpResponse<InputStream> response = client.send(
            request, 
            HttpResponse.BodyHandlers.ofInputStream()
        );
        
        // Копируем поток данных из ответа HTTP в наш временный файл
        // Files.copy() — копирует данные из одного места в другое
        // response.body() — это поток данных из ответа (сам файл)
        // tempFile — куда копируем
        // StandardCopyOption.REPLACE_EXISTING — если файл уже есть, перезаписать его
        Files.copy(response.body(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        
        // Возвращаем путь к скачанному файлу
        // Теперь другой код может использовать этот путь, чтобы прочитать файл
        return tempFile;
    }
}
