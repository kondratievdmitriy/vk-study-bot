package ru.studybot.repository.impl;

import ru.studybot.db.Database;
import ru.studybot.model.Book;
import ru.studybot.repository.BookRepository;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL-реализация хранилища книг (задача 4).
 * <p>
 * Замена InMemoryBookRepository за тем же интерфейсом — команды и Main
 * не знают о способе хранения. Идентификаторы генерирует модель (UUID),
 * поэтому save() работает как upsert по первичному ключу book_id.
 * При чтении id восстанавливается из базы (см. setIdQuietly) — иначе
 * команды, хранящие bookId в прогрессе, не найдут книгу после перезапуска.
 * </p>
 */
public class PgBookRepository implements BookRepository {

    private static final String SAVE_SQL = """
            INSERT INTO books (book_id, title, author, total_lessons)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (book_id) DO UPDATE
                SET title = EXCLUDED.title,
                    author = EXCLUDED.author,
                    total_lessons = EXCLUDED.total_lessons
            """;

    private static final String FIND_ALL_SQL =
            "SELECT book_id, title, author, total_lessons FROM books ORDER BY created_at, book_id";

    private static final String FIND_BY_TITLE_SQL =
            "SELECT book_id, title, author, total_lessons FROM books WHERE title = ?";

    @Override
    public void save(Book book) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(SAVE_SQL)) {
            ps.setString(1, book.getId());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setInt(4, book.getTotalLessons());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить книгу в PostgreSQL", e);
        }
    }

    @Override
    public List<Book> findAll() {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            List<Book> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось прочитать книги из PostgreSQL", e);
        }
    }

    @Override
    public Book findByTitle(String title) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(FIND_BY_TITLE_SQL)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось найти книгу в PostgreSQL", e);
        }
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        Book book = new Book(rs.getString("title"));
        setIdQuietly(book, rs.getString("book_id"));
        book.setAuthor(rs.getString("author"));
        book.setTotalLessons(rs.getInt("total_lessons"));
        return book;
    }

    /** Восстанавливает id из базы: у модели нет сеттера, а перезаписывать модель опасно — гуглится как ломающий чужой код. */
    private static void setIdQuietly(Book book, String id) {
        try {
            Field field = Book.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(book, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось восстановить id книги", e);
        }
    }
}
