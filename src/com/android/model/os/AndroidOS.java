package com.android.model.os;

/**
 * [Lecture 3: OOP Class and Object]
 * [Lecture 4: OOP Concepts - Encapsulation & Abstraction]
 *
 * คลาสระบบปฏิบัติการ Android (Android Operating System Layer)
 * ทำหน้าที่เป็นแกนกลางระบบ (OS Kernel) จัดการข้อมูลเวอร์ชัน, ระดับ API, Uptime,
 * และเป็นเจ้าของอ็อบเจกต์ NotificationManager สำหรับกระจายการแจ้งเตือน
 */
public class AndroidOS {
    private final String versionName;        // ชื่อเวอร์ชัน Android เช่น "Android 15 (Vanilla Ice Cream)"
    private final int apiLevel;               // ระดับ API Level เช่น 35
    private final String kernelVersion;       // เวอร์ชัน Linux Kernel ภายใน
    private final long bootTimeMillis;        // เวลาที่เริ่มบูตเครื่อง (Timestamp ms)
    private final NotificationManager notificationManager; // ระบบจัดการการแจ้งเตือนกลาง

    public AndroidOS(String versionName, int apiLevel) {
        this.versionName = versionName;
        this.apiLevel = apiLevel;
        this.kernelVersion = "Linux 6.6.21-android15-ge91c4d";
        this.bootTimeMillis = System.currentTimeMillis();
        this.notificationManager = new NotificationManager();
    }

    public String getVersionName() {
        return versionName;
    }

    public int getApiLevel() {
        return apiLevel;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    /**
     * คำนวณระยะเวลาเปิดเครื่องต่อเนื่อง (Uptime) ในหน่วยวินาที
     */
    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - bootTimeMillis) / 1000;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
}
