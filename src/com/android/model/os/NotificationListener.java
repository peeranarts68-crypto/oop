package com.android.model.os;

/**
 * Observer interface for listening to system notifications.
 * Demonstrates OOP: Abstraction & Observer Pattern.
 */
public interface NotificationListener {
    void onNotificationPosted(Notification notification);
    void onNotificationsCleared();
}
