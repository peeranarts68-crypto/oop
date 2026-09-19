package com.android.model.os;

/**
 * Data model for system notifications.
 */
public class Notification {
    private final String id;
    private final String title;
    private final String message;
    private final String appSource;
    private final long timestamp;

    public Notification(String id, String title, String message, String appSource) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.appSource = appSource;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getAppSource() { return appSource; }
    public long getTimestamp() { return timestamp; }
}
