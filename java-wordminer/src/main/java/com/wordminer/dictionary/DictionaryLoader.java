package com.wordminer.dictionary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Loads dictionary json files and normalizes them into a lookup map.
 */
public final class DictionaryLoader {

    private static final List<Level> DIFFICULTY_ORDER = List.of(
            new Level("1-middle-school", "Middle School"),
            new Level("2-high-school", "High School"),
            new Level("3-CET4", "CET-4"),
            new Level("4-CET6", "CET-6"),
            new Level("5-postgraduate", "Postgraduate"),
            new Level("6-TOEFL", "TOEFL"),
            new Level("7-SAT", "SAT")
    );

    private DictionaryLoader() {
    }

    public static Map<String, DictionaryEntry> load(Path dictionaryDir) throws IOException {
        if (!Files.isDirectory(dictionaryDir)) {
            return Map.of();
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Path> files = Files.list(dictionaryDir)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparingInt(DictionaryLoader::difficultyIndex))
                .toList();

        Map<String, DictionaryEntry> dictionary = new HashMap<>();
        for (Path file : files) {
            String levelName = resolveLevelName(file.getFileName().toString());
            JsonNode root = mapper.readTree(file.toFile());
            if (!root.isArray()) {
                continue;
            }
            for (JsonNode entryNode : root) {
                JsonNode wordNode = entryNode.get("word");
                if (wordNode == null || wordNode.isNull()) {
                    continue;
                }
                String lemma = normalize(wordNode.asText());
                if (lemma.isEmpty() || dictionary.containsKey(lemma)) {
                    continue;
                }
                List<DictionaryEntry.Translation> translations = new ArrayList<>();
                JsonNode translationsNode = entryNode.get("translations");
                if (translationsNode != null && translationsNode.isArray()) {
                    for (JsonNode tr : translationsNode) {
                        translations.add(new DictionaryEntry.Translation(
                                tr.path("translation").asText(""),
                                tr.path("type").asText("")
                        ));
                    }
                }
                List<DictionaryEntry.Phrase> phrases = new ArrayList<>();
                JsonNode phrasesNode = entryNode.get("phrases");
                if (phrasesNode != null && phrasesNode.isArray()) {
                    for (JsonNode ph : phrasesNode) {
                        phrases.add(new DictionaryEntry.Phrase(
                                ph.path("phrase").asText(""),
                                ph.path("translation").asText("")
                        ));
                    }
                }
                dictionary.put(lemma, new DictionaryEntry(lemma, levelName, translations, phrases));
            }
        }
        return dictionary;
    }

    public static Optional<DictionaryEntry> lookup(Map<String, DictionaryEntry> dictionary, String lemma) {
        if (dictionary == null || lemma == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dictionary.get(normalize(lemma)));
    }

    private static String normalize(String token) {
        return token == null ? "" : token.trim().toLowerCase(Locale.ROOT);
    }

    private static int difficultyIndex(Path path) {
        String filename = path.getFileName().toString();
        for (int i = 0; i < DIFFICULTY_ORDER.size(); i++) {
            if (filename.startsWith(DIFFICULTY_ORDER.get(i).prefix())) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static String resolveLevelName(String filename) {
        for (Level level : DIFFICULTY_ORDER) {
            if (filename.startsWith(level.prefix())) {
                return level.label();
            }
        }
        return "Unknown";
    }

    private record Level(String prefix, String label) {
    }
}
