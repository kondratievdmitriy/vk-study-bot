package ru.studybot.model;

import java.util.UUID;

public class Book {
    private String id;
    private String title;

    public Book(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
}
