package com.wordminer.dictionary;

import java.util.Collections;
import java.util.List;

/**
 * Normalized dictionary entry loaded from the JSON sources.
 */
public record DictionaryEntry(
        String lemma,
        String level,
        List<Translation> translations,
        List<Phrase> phrases) {

    public DictionaryEntry {
        translations = translations == null ? List.of() : List.copyOf(translations);
        phrases = phrases == null ? List.of() : List.copyOf(phrases);
    }

    public List<Translation> translations() {
        return Collections.unmodifiableList(translations);
    }

    public List<Phrase> phrases() {
        return Collections.unmodifiableList(phrases);
    }

    public record Translation(String translation, String type) {
    }

    public record Phrase(String phrase, String translation) {
    }
}
