package ru.studybot;

// Импорты библиотек для работы с PDF
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

// Импорты библиотек для работы с Word (.docx)
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

// Стандартные импорты Java для работы с файлами
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Утилита для чтения текстового содержимого из файлов различных форматов.
 * <p>
 * Этот класс содержит статические методы, поэтому его не нужно создавать через new.
 * Просто вызываем DocumentReader.readFile("путь/к/файлу") и получаем текст.
 * </p>
 * <p>
 * Поддерживаемые форматы:
 * - .txt (обычный текст)
 * - .pdf (PDF документы)
 * - .docx и .doc (Word документы)
 * </p>
 */
public class DocumentReader {

    /**
     * Читает содержимое обычного текстового файла (.txt).
     * <p>
     * Использует Files.readAllBytes() — это самый простой способ прочитать
     * весь файл целиком в массив байт, который потом превращаем в строку.
     * </p>
     *
     * @param filePath путь к файлу (например, "C:/temp/file.txt")
     * @return всё содержимое файла в виде одной строки
     * @throws IOException если файл не найден или не удалось прочитать
     */
    public static String readTextFile(String filePath) throws IOException {
        // Files.readAllBytes() читает весь файл в массив байт
        // Paths.get() превращает строку пути в объект Path
        // new String() превращает массив байт в строку (текст)
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    /**
     * Читает содержимое PDF-файла.
     * <p>
     * Использует библиотеку Apache PDFBox. Она умеет извлекать текст
     * из PDF, даже если там есть картинки и сложное форматирование.
     * </p>
     * <p>
     * Конструкция try-with-resources (try (...)) автоматически закроет
     * документ после чтения, даже если произойдёт ошибка. Это важно,
     * чтобы не было утечек памяти.
     * </p>
     *
     * @param filePath путь к PDF файлу
     * @return текст, извлечённый из PDF
     * @throws IOException если файл повреждён или не является PDF
     */
    public static String readPdfFile(String filePath) throws IOException {
        // PDDocument.load() открывает PDF файл
        // try-with-resources автоматически закроет документ в конце
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            // PDFTextStripper — это "извлекатель текста" из PDF
            PDFTextStripper stripper = new PDFTextStripper();
            // getText() возвращает весь текст из документа
            return stripper.getText(document);
        }
        // document.close() вызовется автоматически благодаря try-with-resources
    }

    /**
     * Читает содержимое Word-файла (.docx).
     * <p>
     * Использует библиотеку Apache POI. Word документы состоят из параграфов,
     * поэтому мы проходим по каждому параграфу и собираем текст в StringBuilder.
     * </p>
     * <p>
     * StringBuilder используется вместо обычной конкатенации строк (+),
     * потому что это быстрее и эффективнее при большом количестве операций.
     * </p>
     *
     * @param filePath путь к Word файлу (.docx)
     * @return текст из всех параграфов документа
     * @throws IOException если файл повреждён или не является Word документом
     */
    public static String readWordFile(String filePath) throws IOException {
        // Открываем поток для чтения файла
        // XWPFDocument — это класс для работы с .docx файлами
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            // StringBuilder — это "строитель строк", эффективнее чем String + String
            StringBuilder text = new StringBuilder();
            
            // Проходим по всем параграфам документа
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                // getText() возвращает текст параграфа
                // append() добавляет текст в наш StringBuilder
                // "\n" — это символ новой строки (Enter)
                text.append(paragraph.getText()).append("\n");
            }
            
            // toString() превращает StringBuilder обратно в обычную строку
            return text.toString();
        }
        // fis.close() и document.close() вызовутся автоматически
    }

    /**
     * Главный метод: определяет расширение файла и вызывает нужный метод чтения.
     * <p>
     * Это метод-диспетчер. Он смотрит на конец имени файла (расширение)
     * и решает, какой метод вызвать для чтения.
     * </p>
     *
     * @param filePath путь к файлу
     * @return текстовое содержимое файла
     * @throws IOException если файл не удалось прочитать
     * @throws IllegalArgumentException если формат файла не поддерживается
     */
    public static String readFile(String filePath) throws IOException {
        // Приводим путь к нижнему регистру, чтобы ".PDF" и ".pdf" работали одинаково
        String lowerPath = filePath.toLowerCase();
        
        // Проверяем расширение файла и вызываем соответствующий метод
        if (lowerPath.endsWith(".txt")) {
            // Если файл заканчивается на .txt — читаем как текст
            return readTextFile(filePath);
        } else if (lowerPath.endsWith(".pdf")) {
            // Если файл заканчивается на .pdf — читаем как PDF
            return readPdfFile(filePath);
        } else if (lowerPath.endsWith(".docx") || lowerPath.endsWith(".doc")) {
            // Если файл заканчивается на .docx или .doc — читаем как Word
            return readWordFile(filePath);
        } else {
            // Если расширение не подходит — выбрасываем ошибку
            // IllegalArgumentException — это стандартное исключение для "неправильный аргумент"
            throw new IllegalArgumentException(
                "Неподдерживаемый формат файла. Поддерживаются: .txt, .pdf, .docx"
            );
        }
    }
}