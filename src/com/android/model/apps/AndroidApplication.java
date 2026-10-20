package com.android.model.apps;

import java.util.Map;

/**
 * [Lecture 4: OOP Concepts - Abstraction & Polymorphism]
 *
 * คลาสแม่แบบแอบสแตรกต์สำหรับแอปพลิเคชัน Android ทั้งหมดในระบบ (Abstract Base Class)
 * - Abstraction: กำหนดข้อกำหนดวงจรชีวิตของ Activity (Lifecycle State Contract)
 * - Polymorphism: บังคับให้แต่ละแอปพลิเคชันย่อย (เช่น PsruLostFoundApp, SettingsApp)
 *   ต้อง Override เมธอด executeAction() และ getAppData() เพื่อเขียนพฤติกรรมเฉพาะของตนเอง
 */
public abstract class AndroidApplication {
    /**
     * สถานะวงจรชีวิตของแอปพลิเคชันตามมาตรฐาน Android Activity Lifecycle
     */
    public enum LifecycleState {
        NOT_INITIALIZED, // ยังไม่เริ่มระบบ
        CREATED,         // ถูกสร้างขึ้นในหน่วยความจำ (onCreate)
        STARTED,         // มองเห็นหน้าจอแอป (onStart)
        RESUMED,         // กำลังทำงานและพร้อมรับการสัมผัสจากผู้ใช้ (onResume)
        PAUSED,          // แอปถูกพักการทำงานชั่วคราว (onPause)
        STOPPED,         // แอปถูกย่อลงพื้นหลัง (onStop)
        DESTROYED        // แอปถูกปิดและคืนหน่วยความจำ (onDestroy)
    }

    protected final String appId;       // รหัส Package ของแอป เช่น "com.psru.lostfound"
    protected final String appName;     // ชื่อของแอปพลิเคชันที่แสดงบนหน้าจอ
    protected final String icon;        // ชื่อไอคอนหรือคีย์ระบุรูปไอคอน
    protected final String version;     // เวอร์ชันของแอป เช่น "1.0.0"
    protected final int sizeMb;         // ขนาดพื้นที่ติดตั้งของแอป (MB)
    protected LifecycleState state;     // สถานะ Lifecycle ปัจจุบันของแอป

    public AndroidApplication(String appId, String appName, String icon, String version, int sizeMb) {
        this.appId = appId;
        this.appName = appName;
        this.icon = icon;
        this.version = version;
        this.sizeMb = sizeMb;
        this.state = LifecycleState.NOT_INITIALIZED;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppName() {
        return appName;
    }

    public String getIcon() {
        return icon;
    }

    public String getVersion() {
        return version;
    }

    public int getSizeMb() {
        return sizeMb;
    }

    public LifecycleState getState() {
        return state;
    }

    // --- เมธอดวงจรชีวิต Activity Lifecycle (Android Lifecycle Callbacks) ---

    public void onCreate() {
        this.state = LifecycleState.CREATED;
    }

    public void onStart() {
        this.state = LifecycleState.STARTED;
    }

    public void onResume() {
        this.state = LifecycleState.RESUMED;
    }

    public void onPause() {
        this.state = LifecycleState.PAUSED;
    }

    public void onStop() {
        this.state = LifecycleState.STOPPED;
    }

    public void onDestroy() {
        this.state = LifecycleState.DESTROYED;
    }

    /**
     * เมธอดแอบสแตรกต์ประมวลผลคำสั่งของแอป (Polymorphism)
     * คลาสลูกแต่ละแอปต้องไปเขียนโค้ดตรรกะทางธุรกิจ (Business Logic) ของตนเอง
     * @param action คำสั่งการทำงาน เช่น "LOGIN", "CREATE_POST"
     * @param params พารามิเตอร์ข้อมูลที่ส่งมาจากผู้ใช้
     * @return ผลลัพธ์การทำงาน ActionResult
     */
    public abstract ActionResult executeAction(String action, Map<String, Object> params);

    /**
     * ส่งคืนข้อมูลสถานะภายในของแอป เพื่อนำไปแสดงผลบนหน้าจอ UI
     */
    public abstract Object getAppData();

    /**
     * ผลลัพธ์จากการทำงานของ Action (Result Wrapper)
     */
    public static class ActionResult {
        private final boolean success; // สถานะสำเร็จหรือไม่ (true = สำเร็จ)
        private final String message;  // ข้อความแจ้งเตือนผลลัพธ์
        private final Object data;     // ข้อมูล Payload ผลลัพธ์

        public ActionResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public String message() { return message; }
        public Object getData() { return data; }
    }
}
