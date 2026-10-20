package com.android.model.os;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * [Lecture 5.1: Event-driven Programming]
 * [Lecture 4: OOP Concepts - Encapsulation & Observer Pattern]
 *
 * ตัวจัดการการแจ้งเตือนของระบบ (System Notification Manager)
 * ทำหน้าที่เป็น "Subject" ในรูปแบบ Observer Pattern คอยรับข้อความแจ้งเตือนจากแอปต่างๆ
 * เก็บประวัติ และกระจายการแจ้งเตือน (Broadcast) ไปยังผู้ฟัง (Listeners)
 */
public class NotificationManager {
    private final List<Notification> activeNotifications; // รายการแจ้งเตือนที่เปิดอยู่
    private final List<NotificationListener> listeners;    // รายการผู้ฟัง (Observers)
    private int notificationCounter = 0;                  // ตัวนับลำดับการแจ้งเตือน

    public NotificationManager() {
        this.activeNotifications = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    /**
     * ลงทะเบียนผู้ฟังรับการแจ้งเตือน
     */
    public void registerListener(NotificationListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * ยกเลิกการลงทะเบียนผู้ฟัง
     */
    public void unregisterListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    /**
     * ส่งการแจ้งเตือนใหม่เข้าสู่ระบบ
     * @param title หัวข้อการแจ้งเตือน
     * @param message เนื้อหาข้อความ
     * @param appSource ชื่อแอปพลิเคชันต้นทางที่ส่ง
     */
    public synchronized void postNotification(String title, String message, String appSource) {
        notificationCounter++;
        Notification notification = new Notification("notif_" + notificationCounter, title, message, appSource);
        activeNotifications.add(0, notification); // แทรกไว้ด้านบนสุดเสมอ
        
        // แจ้งเตือนไปยัง Observers ทุกตัว
        for (NotificationListener listener : listeners) {
            listener.onNotificationPosted(notification);
        }
    }

    /**
     * ล้างประวัติการแจ้งเตือนทั้งหมด
     */
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
