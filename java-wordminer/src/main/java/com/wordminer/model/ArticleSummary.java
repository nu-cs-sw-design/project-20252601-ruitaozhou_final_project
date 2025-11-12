package com.wordminer.model;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight projection of an article row for list displays.
 */
public record ArticleSummary(int id, String title, String path, String createdAt) {

    public String displayLabel() {
        return "[" + id + "] " + title;
    }

    public OffsetDateTime createdAtAsTime() {
        return OffsetDateTime.parse(createdAt);
    }

    @Override
    public String toString() {
        return displayLabel();
    }
}
