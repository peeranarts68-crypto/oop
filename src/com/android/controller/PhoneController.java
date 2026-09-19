package com.android.controller;

import com.android.events.PhoneEventListener;
import com.android.model.AndroidPhone;
import com.android.model.apps.AndroidApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * [Lecture 5.2: MVC Architectural Pattern - Controller]
 *
 * คลาส Controller ควบคุมการทำงานของโทรศัพท์ (เชื่อมต่อระหว่าง View และ Model)
 * ทำหน้าที่รับคำสั่งจากผู้ใช้ (เช่น กดปุ่ม Power, Volume, Home, Back, เปิดแอป)
 * แล้วสั่งการไปยัง Model (AndroidPhone) พร้อมแจ้งเตือน Event ไปยัง Listener
 */
public class PhoneController {
    private final AndroidPhone phone;
    private final List<PhoneEventListener> listeners;

    public PhoneController(AndroidPhone phone) {
        this.phone = phone;
        this.listeners = new ArrayList<>();
    }

    // ลงทะเบียน Event Listener [Lecture 5.1]
    public void addListener(PhoneEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // เมธอดควบคุมการเปิด/ปิดเครื่อง
    public boolean togglePower() {
        phone.togglePower();
        boolean state = phone.isPoweredOn();
        for (PhoneEventListener l : listeners) {
            l.onPowerChanged(state);
        }
        return state;
    }

    // เมธอดปรับระดับเสียงขึ้น
    public int volumeUp() {
        phone.volumeUp();
        int vol = phone.getVolume();
        for (PhoneEventListener l : listeners) {
            l.onVolumeChanged(vol);
        }
        return vol;
    }

    // เมธอดปรับระดับเสียงลง
    public int volumeDown() {
        phone.volumeDown();
        int vol = phone.getVolume();
        for (PhoneEventListener l : listeners) {
            l.onVolumeChanged(vol);
        }
        return vol;
    }

    // เมธอดเปิดแอปพลิเคชัน
    public boolean launchApp(String appId) {
        boolean ok = phone.launchApp(appId);
        if (ok) {
            AndroidApplication app = phone.getInstalledApps().get(appId);
            String name = (app != null) ? app.getAppName() : appId;
            for (PhoneEventListener l : listeners) {
                l.onAppLaunched(name);
            }
        }
        return ok;
    }

    // เมธอดปิดแอปพลิเคชันปัจจุบัน
    public void closeCurrentApp() {
        phone.closeCurrentApp();
        for (PhoneEventListener l : listeners) {
            l.onAppClosed("Current App");
        }
    }

    // เมธอดปุ่ม Home
    public void pressHome() {
        phone.pressHomeButton();
    }

    // เมธอดปุ่ม Back
    public void pressBack() {
        phone.pressBackButton();
    }

    // Getter คืนค่า Model สำหรับนำไปแสดงผลใน View
    public AndroidPhone getPhoneModel() {
        return phone;
    }
}
