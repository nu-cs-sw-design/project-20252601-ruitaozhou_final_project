package com.wordminer.model;

/**
 * Learner-controlled familiarity buckets for individual lemmas.
 */
public enum VocabStatus {
    MASTERED("mastered"),
    LEARNING("learning"),
    UNFAMILIAR("unfamiliar");

    private final String dbValue;

    VocabStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static VocabStatus fromDb(String value) {
        for (VocabStatus status : values()) {
            if (status.dbValue.equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }
}
