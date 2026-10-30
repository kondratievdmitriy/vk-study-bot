package ru.studybot.repository.impl;

import ru.studybot.repository.BookRepository;
import ru.studybot.model.Book;
import java.util.*;

public class InMemoryBookRepository implements BookRepository {
    private final List<Book> books = new ArrayList<>();

    @Override
    public void save(Book book) {
        books.removeIf(b -> b.getTitle().equals(book.getTitle()));
        books.add(book);
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(books);
    }

    @Override
    public Book findByTitle(String title) {
        for (Book b : books) {
            if (b.getTitle().equals(title)) return b;
        }
        return null;
    }
}
