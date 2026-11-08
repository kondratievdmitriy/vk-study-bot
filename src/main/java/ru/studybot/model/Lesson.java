package ru.studybot.model;

import java.util.UUID;

public class Lesson {
    private String id;
    private String bookId;
    private int lessonNumber;
    private String topic;
    private String content;
    private String questions; // JSON с вопросами — заполняется в задаче 3

    public Lesson(String bookId, int lessonNumber, String topic, String content) {
        this.id = UUID.randomUUID().toString();
        this.bookId = bookId;
        this.lessonNumber = lessonNumber;
        this.topic = topic;
        this.content = content;
    }

    public String getId() { return id; }
    public String getBookId() { return bookId; }
    public int getLessonNumber() { return lessonNumber; }
    public String getTopic() { return topic; }
    public String getContent() { return content; }
    public String getQuestions() { return questions; }
    public void setQuestions(String questions) { this.questions = questions; }
}
