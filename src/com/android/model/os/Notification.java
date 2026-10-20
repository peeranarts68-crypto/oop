package com.android.model.os;

/**
 * [Lecture 3: OOP Class and Object]
 *
 * โมเดลข้อมูลการแจ้งเตือน (System Notification Model)
 * ห่อหุ้มข้อมูลรหัสแจ้งเตือน, หัวข้อ, ข้อความ, แหล่งที่มาของแอป และเวลาที่เกิดขึ้น
 */
public class Notification {
    private final String id;         // รหัสเฉพาะของการแจ้งเตือน เช่น "notif_1"
    private final String title;      // หัวข้อการแจ้งเตือน
    private final String message;    // ข้อความรายละเอียด
    private final String appSource;  // ชื่อแอปพลิเคชันที่ส่ง เช่น "PSRU Lost & Found"
    private final long timestamp;    // เวลาที่เกิดการแจ้งเตือน (Timestamp ms)

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
