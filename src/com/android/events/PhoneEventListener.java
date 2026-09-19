package com.android.events;

/**
 * [Lecture 5.1: Event-driven Programming]
 *
 * อินเทอร์เฟซสำหรับรับฟังเหตุการณ์ (Event Listener) การทำงานของโทรศัพท์
 * เช่น การเปิด-ปิดเครื่อง, การเปลี่ยนระดับเสียง, หรือการเปิด-ปิดแอปพลิเคชัน
 */
public interface PhoneEventListener {
    void onPowerChanged(boolean isPoweredOn);
    void onVolumeChanged(int newVolume);
    void onAppLaunched(String appName);
    void onAppClosed(String appName);
}
