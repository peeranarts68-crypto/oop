package com.android.model.os;

/**
 * [Lecture 5.1: Event-driven Programming]
 * [Lecture 4: OOP Concepts - Abstraction & Observer Pattern]
 *
 * อินเทอร์เฟซผู้ฟังเหตุการณ์การแจ้งเตือน (Notification Observer Interface)
 * กำหนดเมธอด Callback ให้คลาสใดก็ตามที่ต้องการรับทราบเมื่อมีการแจ้งเตือนใหม่เข้ามาในระบบ
 */
public interface NotificationListener {
    /** ทำงานเมื่อมี Notification ใหม่ถูกโพสต์ */
    void onNotificationPosted(Notification notification);

    /** ทำงานเมื่อมีการกดล้าง Notification ทั้งหมด */
    void onNotificationsCleared();
}
