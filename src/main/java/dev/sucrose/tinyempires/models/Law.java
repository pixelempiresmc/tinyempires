package dev.sucrose.tinyempires.models;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class Law {

    private final List<String> content = new ArrayList<>();
    private final String author;

    public Law(Document document) {
        content.addAll(document.getList("content", String.class));
        author = document.getString("author");
    }

    public Law(List<String> content, String author) {
        this.content.addAll(content);
        this.author = author;
    }

    public void update(List<String> content) {
        this.content.clear();
        this.content.addAll(content);
    }

    public Document toDocument() {
        return new Document("content", content)
            .append("author", author);
    }

    public List<String> getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

}
