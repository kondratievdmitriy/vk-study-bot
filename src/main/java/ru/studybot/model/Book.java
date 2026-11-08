package ru.studybot.model;

import java.util.UUID;

public class Book {
    private String id;
    private String title;
    private String author;
    private int totalLessons;

    public Book(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.totalLessons = 0;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getTotalLessons() { return totalLessons; }
    public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }
}
