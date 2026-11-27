package ru.studybot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты определения расширения файла по URL и по Content-Type:
 * query-параметры игнорируются, кириллица в пути не ломает разбор,
 * неподдерживаемые расширения отвергаются, отсутствие расширения
 * трактуется как TXT.
 */
class FileDownloaderTest {

    /** Расширение берётся из пути, query-параметры игнорируются. */
    @Test
    void extractsPdfExtensionIgnoringQuery() {
        assertEquals(".pdf", FileDownloader.extensionOf("https://vk.com/doc1_2.pdf?hash=abc&dl=1"));
    }

    /** Кириллица в имени файла не мешает разбору расширения. */
    @Test
    void extractsDocxExtension() {
        assertEquals(".docx", FileDownloader.extensionOf("https://example.com/files/лекция.docx"));
    }

    /** Нет расширения — по умолчанию TXT. */
    @Test
    void defaultsToTxtWhenNoExtension() {
        assertEquals(".txt", FileDownloader.extensionOf("https://vk.com/doc1_2?hash=abc"));
    }

    /** Неподдерживаемое расширение (jpg) — IllegalArgumentException. */
    @Test
    void rejectsUnsupportedExtension() {
        assertThrows(IllegalArgumentException.class,
                () -> FileDownloader.extensionOf("https://example.com/photo.jpg"));
    }

    /** Расширение определяется и по Content-Type (с charset и без);
     *  пустой и неопределённый тип дают null. */
    @Test
    void detectsExtensionFromContentType() {
        assertEquals(".pdf", FileDownloader.extensionFromContentType("application/pdf"));
        assertEquals(".txt", FileDownloader.extensionFromContentType("text/plain; charset=utf-8"));
        assertNull(FileDownloader.extensionFromContentType(""));
        assertNull(FileDownloader.extensionFromContentType("application/octet-stream"));
    }
}

