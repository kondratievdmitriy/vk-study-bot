package ru.studybot.repository;

import ru.studybot.model.Book;
import java.util.List;

public interface BookRepository {
    void save(Book book);
    List<Book> findAll();
    Book findByTitle(String title);
}
