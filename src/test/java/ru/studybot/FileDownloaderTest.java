package ru.studybot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileDownloaderTest {

    @Test
    void extractsPdfExtensionIgnoringQuery() {
        assertEquals(".pdf", FileDownloader.extensionOf("https://vk.com/doc1_2.pdf?hash=abc&dl=1"));
    }

    @Test
    void extractsDocxExtension() {
        assertEquals(".docx", FileDownloader.extensionOf("https://example.com/files/лекция.docx"));
    }

    @Test
    void defaultsToTxtWhenNoExtension() {
        assertEquals(".txt", FileDownloader.extensionOf("https://vk.com/doc1_2?hash=abc"));
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThrows(IllegalArgumentException.class,
                () -> FileDownloader.extensionOf("https://example.com/photo.jpg"));
    }
}
