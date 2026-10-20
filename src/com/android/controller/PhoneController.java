package com.android.controller;

import com.android.events.PhoneEventListener;
import com.android.model.AndroidPhone;
import com.android.model.apps.AndroidApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * [Lecture 5.2: MVC Architectural Pattern - Controller (ส่วนควบคุม)]
 * ============================================================================
 * 
 * คลาส PhoneController ทำหน้าที่เป็น Controller ควบคุมการทำงานของโทรศัพท์
 * เชื่อมโยงระหว่างมุมมองหน้าจอ (View / Web GUI) กับโมเดลฮาร์ดแวร์ (Model / AndroidPhone)
 * 
 * หน้าที่หลักตามหลัก MVC:
 * 1. รับคำสั่งจากผู้ใช้ (เช่น กดปุ่มเปิดเครื่อง, ปรับระดับเสียง, เปิด-ปิดแอป)
 * 2. เรียกใช้งานเมธอดใน Model (AndroidPhone) เพื่อเปลี่ยนแปลงสถานะฮาร์ดแวร์
 * 3. แจ้งเตือนเหตุการณ์ไปยัง PhoneEventListener (Event-Driven Programming [Lecture 5.1])
 * 4. คืนค่าผลลัพธ์เพื่อนำไปอัปเดตหน้าจอ View ให้สอดคล้องกัน
 * 
 * @author CPE Student
 * @version 1.0
 */
public class PhoneController {

    /** อ็อบเจกต์ Model โทรศัพท์ที่อยู่ภายใต้การควบคุม */
    private final AndroidPhone phone;

    /** รายชื่อผู้รับฟังเหตุการณ์การทำงานของโทรศัพท์ [Lecture 5.1] */
    private final List<PhoneEventListener> listeners;

    /**
     * [Lecture 3: Constructor]
     * ตัวสร้าง PhoneController
     * @param phone โมเดลโทรศัพท์ AndroidPhone
     */
    public PhoneController(AndroidPhone phone) {
        this.phone = phone;
        this.listeners = new ArrayList<>();
    }

    /**
     * ลงทะเบียนผู้รับฟังเหตุการณ์โทรศัพท์ [Lecture 5.1: Event-driven]
     * @param listener ผู้รับฟังเหตุการณ์
     */
    public void addListener(PhoneEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * คำสั่งสลับเปิด/ปิดเครื่อง (Power Button)
     * สั่งการไปยัง Model และกระจาย Event onPowerChanged ไปยัง Listeners
     * @return สถานะการเปิดเครื่องหลังสั่งการ (true = เปิดอยู่, false = ปิดอยู่)
     */
    public boolean togglePower() {
        phone.togglePower();
        boolean state = phone.isPoweredOn();
        for (PhoneEventListener l : listeners) {
            l.onPowerChanged(state);
        }
        return state;
    }

    /**
     * คำสั่งเร่งเสียงขึ้น 10% (Volume Up Button)
     * @return ระดับเสียงใหม่หลังปรับ (0 - 100)
     */
    public int volumeUp() {
        phone.volumeUp();
        int vol = phone.getVolume();
        for (PhoneEventListener l : listeners) {
            l.onVolumeChanged(vol);
        }
        return vol;
    }

    /**
     * คำสั่งลดเสียงลง 10% (Volume Down Button)
     * @return ระดับเสียงใหม่หลังปรับ (0 - 100)
     */
    public int volumeDown() {
        phone.volumeDown();
        int vol = phone.getVolume();
        for (PhoneEventListener l : listeners) {
            l.onVolumeChanged(vol);
        }
        return vol;
    }

    /**
     * คำสั่งเปิดแอปพลิเคชัน (Launch App)
     * สั่งเปิดแอปใน Model และแจ้ง Event onAppLaunched
     * @param appId รหัสประจำแอป เช่น "com.psru.lostfound"
     * @return true ถ้าเปิดสำเร็จ
     */
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

    /**
     * คำสั่งปิดแอปพลิเคชันปัจจุบันที่กำลังแสดงผลอยู่
     */
    public void closeCurrentApp() {
        phone.closeCurrentApp();
        for (PhoneEventListener l : listeners) {
            l.onAppClosed("Current App");
        }
    }

    /**
     * คำสั่งกดปุ่มโฮม (Home Button) กลับสู่หน้าแรก
     */
    public void pressHome() {
        phone.pressHomeButton();
    }

    /**
     * คำสั่งกดปุ่มย้อนกลับ (Back Button)
     */
    public void pressBack() {
        phone.pressBackButton();
    }

    /**
     * อ่านโมเดล AndroidPhone เพื่อนำไปแสดงผลบน View
     * @return AndroidPhone
     */
    public AndroidPhone getPhoneModel() {
        return phone;
    }
}
