package com.wordminer.model;

/**
 * Represents aggregated vocabulary counts for a single lemma.
 */
public record TokenStat(String lemma, int count, String difficulty) {
}
