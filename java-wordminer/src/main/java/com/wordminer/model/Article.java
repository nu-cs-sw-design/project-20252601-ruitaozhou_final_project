package com.wordminer.model;

/**
 * Full article record with content payload.
 */
public record Article(int id, String title, String path, String content, String createdAt) {
}
