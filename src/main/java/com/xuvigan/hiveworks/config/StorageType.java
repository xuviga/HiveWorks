package com.xuvigan.hiveworks.config;

import java.util.Locale;

public enum StorageType {
    YAML,
    SQLITE;

    public static StorageType parse(String raw) {
        if (raw == null) {
            return SQLITE;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return SQLITE;
        }
    }
}
