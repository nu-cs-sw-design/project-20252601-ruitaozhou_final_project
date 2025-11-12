package com.wordminer.analysis;

import com.wordminer.dictionary.DictionaryEntry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizes and annotates article content in a lightweight way.
 */
public final class TextAnalyzer {

    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z]+");

    public AnalysisResult analyze(String content, Map<String, DictionaryEntry> dictionary) {
        Map<String, Integer> lemmaCounts = new HashMap<>();
        Map<String, Integer> difficultyCounts = new HashMap<>();

        Matcher matcher = WORD_PATTERN.matcher(content);
        while (matcher.find()) {
            String token = matcher.group();
            String lemma = lemmatize(token);
            lemmaCounts.merge(lemma, 1, Integer::sum);
            DictionaryEntry entry = dictionary == null ? null : dictionary.get(lemma);
            String level = entry == null ? "Unknown" : entry.level();
            difficultyCounts.merge(level, 1, Integer::sum);
        }
        return new AnalysisResult(lemmaCounts, difficultyCounts);
    }

    public Matcher matcher(String content) {
        return WORD_PATTERN.matcher(content);
    }

    public String lemmatize(String token) {
        if (token == null) {
            return "";
        }
        String t = token.toLowerCase(Locale.ROOT);
        if (t.length() > 3) {
            if (t.endsWith("ies")) {
                return t.substring(0, t.length() - 3) + "y";
            }
            if (t.endsWith("ing") && t.length() > 5) {
                String base = t.substring(0, t.length() - 3);
                if (base.length() > 1 && base.endsWith(base.substring(base.length() - 1))) {
                    base = base.substring(0, base.length() - 1);
                }
                return base;
            }
            if (t.endsWith("ed") && t.length() > 4) {
                String base = t.substring(0, t.length() - 2);
                if (base.length() > 1 && base.endsWith(base.substring(base.length() - 1))) {
                    base = base.substring(0, base.length() - 1);
                }
                return base;
            }
            if (t.endsWith("es") && t.length() > 4) {
                return t.substring(0, t.length() - 2);
            }
        }
        if (t.endsWith("s") && t.length() > 3) {
            return t.substring(0, t.length() - 1);
        }
        return t;
    }
}
