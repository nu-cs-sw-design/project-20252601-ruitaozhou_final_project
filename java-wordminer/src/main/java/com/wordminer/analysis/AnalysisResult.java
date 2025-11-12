package com.wordminer.analysis;

import java.util.Map;

/**
    Aggregated output capturing lemma frequency and difficulty buckets.
 */
public record AnalysisResult(Map<String, Integer> lemmaCounts, Map<String, Integer> difficultyCounts) {
}
