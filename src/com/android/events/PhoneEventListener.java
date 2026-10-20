package com.android.events;

/**
 * ============================================================================
 * [Lecture 5.1: Event-driven Programming & Observer Pattern]
 * ============================================================================
 * 
 * อินเทอร์เฟซสำหรับรับฟังเหตุการณ์ (Event Listener) ของฮาร์ดแวร์โทรศัพท์
 * ทำหน้าที่เป็น Observer คอยรับ Callback เมื่อสถานะของโทรศัพท์เปลี่ยนไปใน PhoneController
 * 
 * @author CPE Student
 * @version 1.0
 */
public interface PhoneEventListener {

    /**
     * เรียกใช้งานเมื่อมีการกดปุ่มเปิด/ปิดเครื่อง (Power State Changed)
     * @param isPoweredOn true หากเครื่องเปิดอยู่, false หากเครื่องปิดอยู่
     */
    void onPowerChanged(boolean isPoweredOn);

    /**
     * เรียกใช้งานเมื่อมีการปรับระดับเสียงลำโพง (Volume Level Changed)
     * @param newVolume ระดับเสียงใหม่ (ช่วง 0 - 100%)
     */
    void onVolumeChanged(int newVolume);

    /**
     * เรียกใช้งานเมื่อมีการเปิดแอปพลิเคชันขึ้นสู่หน้าจอ
     * @param appName ชื่อของแอปพลิเคชันที่เปิดใช้งาน
     */
    void onAppLaunched(String appName);

    /**
     * เรียกใช้งานเมื่อมีการปิดแอปพลิเคชัน
     * @param appName ชื่อของแอปพลิเคชันที่ถูกปิด
     */
    void onAppClosed(String appName);
}
