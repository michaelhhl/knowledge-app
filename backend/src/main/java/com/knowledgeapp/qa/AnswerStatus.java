package com.knowledgeapp.qa;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum AnswerStatus {
    GROUNDED,
    INSUFFICIENT,
    REFUSED;

    @JsonValue
    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AnswerStatus fromWire(String value) {
        if (value == null) {
            return INSUFFICIENT;
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "grounded" -> GROUNDED;
            case "refused" -> REFUSED;
            default -> INSUFFICIENT;
        };
    }
}
