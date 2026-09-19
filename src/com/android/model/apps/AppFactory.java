package com.android.model.apps;

import com.android.model.AndroidPhone;

/**
 * Factory class for creating Android Application instances.
 * Demonstrates OOP: Factory Design Pattern.
 */
public class AppFactory {

    public static AndroidApplication createApp(String type, AndroidPhone phone) {
        if (type == null) {
            throw new IllegalArgumentException("App type cannot be null");
        }

        return switch (type.toLowerCase()) {
            case "settings", "com.android.settings" -> new SettingsApp(phone);
            case "psru", "lost_and_found", "com.psru.lostfound" -> new PsruLostFoundApp(phone);
            default -> throw new IllegalArgumentException("Unsupported application type: " + type);
        };
    }
}
