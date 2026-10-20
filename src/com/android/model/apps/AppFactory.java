package com.android.model.apps;

import com.android.model.AndroidPhone;

/**
 * ============================================================================
 * [Design Pattern: Factory Pattern (รูปแบบโรงงานสร้างอ็อบเจกต์)]
 * ============================================================================
 * 
 * คลาส AppFactory ทำหน้าที่เป็นศูนย์กลางในการสร้าง Instance ของแอปพลิเคชัน (AndroidApplication)
 * โดยผู้เรียกใช้ไม่จำเป็นต้องรู้รายละเอียด Constructor หรือคลาสที่แท้จริง
 * เพียงแค่ระบุประเภท (type) ของแอปพลิเคชันที่ต้องการ
 * 
 * ข้อดีตามหลัก OOP:
 * 1. Loose Coupling: ลดการผูกติดระหว่างโค้ดส่วนสร้างกับคลาสที่ถูกสร้าง
 * 2. Polymorphism: คืนค่าเป็น AndroidApplication ซึ่งเป็นคลาสแม่
 * 3. Extensibility: หากมีแอปพลิเคชันใหม่ สามารถเพิ่มเคสใน switch ได้ทันที
 * 
 * @author CPE Student
 * @version 1.0
 */
public class AppFactory {

    /**
     * เมธอดคงที่ (Static Factory Method) สำหรับสร้างแอปพลิเคชันตามประเภทที่ระบุ
     * 
     * @param type ชนิดหรือ App ID ของแอปพลิเคชัน เช่น "settings", "psru", "com.psru.lostfound"
     * @param phone อ้างอิงสมาร์ตโฟนที่เป็นเจ้าของแอป
     * @return อินสแตนซ์ของ AndroidApplication ที่ถูกสร้างขึ้น
     * @throws IllegalArgumentException หากไม่ระบุ type หรือ type ที่ระบุไม่รองรับ
     */
    public static AndroidApplication createApp(String type, AndroidPhone phone) {
        if (type == null) {
            throw new IllegalArgumentException("App type cannot be null (ไม่สามารถระบุชนิดแอปเป็น null ได้)");
        }

        return switch (type.toLowerCase()) {
            case "settings", "com.android.settings" -> new SettingsApp(phone);
            case "psru", "lost_and_found", "com.psru.lostfound" -> new PsruLostFoundApp(phone);
            default -> throw new IllegalArgumentException("Unsupported application type: " + type + " (ไม่พบแอปพลิเคชันประเภทนี้)");
        };
    }
}
