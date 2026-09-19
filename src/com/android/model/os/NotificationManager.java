package com.android.model.os;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages system notifications.
 * Demonstrates OOP: Observer Pattern (Subject) & Encapsulation.
 */
public class NotificationManager {
    private final List<Notification> activeNotifications;
    private final List<NotificationListener> listeners;
    private int notificationCounter = 0;

    public NotificationManager() {
        this.activeNotifications = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public void registerListener(NotificationListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    public synchronized void postNotification(String title, String message, String appSource) {
        notificationCounter++;
        Notification notification = new Notification("notif_" + notificationCounter, title, message, appSource);
        activeNotifications.add(0, notification); // add at top
        
        // Notify observers
        for (NotificationListener listener : listeners) {
            listener.onNotificationPosted(notification);
        }
    }

    public synchronized void clearAll() {
        activeNotifications.clear();
        for (NotificationListener listener : listeners) {
            listener.onNotificationsCleared();
        }
    }

    public List<Notification> getActiveNotifications() {
        return Collections.unmodifiableList(activeNotifications);
    }

    public int getNotificationCount() {
        return activeNotifications.size();
    }
}
