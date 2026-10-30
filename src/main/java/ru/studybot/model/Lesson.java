package ru.studybot.model;

import java.util.UUID;

public class Lesson {
    private String id;
    private String bookId;
    private String topic;
    private String content;

    public Lesson(String bookId, String topic, String content) {
        this.id = UUID.randomUUID().toString();
        this.bookId = bookId;
        this.topic = topic;
        this.content = content;
    }

    public String getId() { return id; }
    public String getBookId() { return bookId; }
    public String getTopic() { return topic; }
    public String getContent() { return content; }
}
