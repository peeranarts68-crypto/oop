package com.android.model;

import com.android.model.apps.AndroidApplication;
import com.android.model.os.AndroidOS;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * [Lecture 4: OOP Concepts - Inheritance (การสืบทอด) & Polymorphism (การพ้องรูป)]
 * ============================================================================
 * 
 * คลาสรูปธรรม (Concrete Class) ของสมาร์ตโฟนระบบปฏิบัติการแอนดรอยด์ (AndroidPhone)
 * - สืบทอด (extends) มาจาก SmartPhone
 * - นำระบบปฏิบัติการ AndroidOS มาควบคุมการทำงาน
 * - จัดการรายการแอปพลิเคชันที่ติดตั้งในเครื่อง (installedApps)
 * - ควบคุม Activity Lifecycle ของแอปที่กำลังเปิดใช้งาน (currentRunningApp)
 * - Override เมธอด powerOn() และ powerOff() ให้เปิด/ปิดจอแสดงผลและส่งแจ้งเตือน
 * 
 * @author CPE Student
 * @version 1.0
 */
public class AndroidPhone extends SmartPhone {

    /** ระบบปฏิบัติการ Android ประจำเครื่อง */
    private final AndroidOS os;

    /** ตารางเก็บแอปที่ติดตั้งในเครื่อง โดยใช้ Map<appId, AndroidApplication> */
    private final Map<String, AndroidApplication> installedApps;

    /** อ้างอิงแอปพลิเคชันที่กำลังเปิดทำงานอยู่บนหน้าจอ (Foreground App) */
    private AndroidApplication currentRunningApp;

    /** รายการประวัติแอปที่เปิดล่าสุด (Recent Apps) */
    private final List<String> recentAppIds;

    /**
     * [Lecture 3: Constructor]
     * ตัวสร้าง AndroidPhone พร้อมติดตั้งระบบปฏิบัติการ AndroidOS
     * 
     * @param brand ยี่ห้อ
     * @param model ชื่อรุ่น
     * @param serialNumber หมายเลขเครื่อง
     * @param batteryMah แบตเตอรี่ (mAh)
     * @param resolution ความละเอียดหน้าจอ
     * @param screenInches ขนาดหน้าจอ (นิ้ว)
     * @param storageMb พื้นที่จัดเก็บข้อมูล (MB)
     * @param cameraMp ความละเอียดกล้อง (ล้านพิกเซล)
     * @param androidVersion ชื่อเวอร์ชันแอนดรอยด์ เช่น "Android 15"
     * @param apiLevel หมายเลข API Level เช่น 35
     */
    public AndroidPhone(String brand, String model, String serialNumber,
                        int batteryMah, String resolution, double screenInches,
                        int storageMb, int cameraMp, String androidVersion, int apiLevel) {
        // เรียก Constructor ของ SmartPhone
        super(brand, model, serialNumber, batteryMah, resolution, screenInches, storageMb, cameraMp);
        this.os = new AndroidOS(androidVersion, apiLevel);
        this.installedApps = new LinkedHashMap<>();
        this.currentRunningApp = null;
        this.recentAppIds = new ArrayList<>();
        this.isPoweredOn = true; // ค่าเริ่มต้นเปิดเครื่องพร้อมใช้งาน
    }

    // ============================================================================
    // [Lecture 4: Polymorphism - Method Overriding จาก Device]
    // ============================================================================

    /**
     * สั่งเปิดเครื่องสมาร์ตโฟน
     * 1. ตรวจสอบว่าแบตเตอรี่หมดหรือไม่ (ถ้าหมด จะเปิดไม่ติด)
     * 2. เปิดหน้าจอแสดงผล (display.turnOn())
     * 3. ส่ง Notification ต้อนรับผู้ใช้เข้าสู่ระบบ Android
     * @return true ถ้าเปิดเครื่องสำเร็จ
     */
    @Override
    public boolean powerOn() {
        if (battery.isDepleted()) {
            return false;
        }
        this.isPoweredOn = true;
        this.display.turnOn();
        os.getNotificationManager().postNotification(
            "System Started",
            "Welcome to " + getModel() + " running " + os.getVersionName(),
            "Android System"
        );
        return true;
    }

    /**
     * สั่งปิดเครื่องสมาร์ตโฟน
     * 1. ปิดแอปพลิเคชันที่เปิดค้างอยู่และเรียก onDestroy()
     * 2. ดับหน้าจอแสดงผล (display.turnOff())
     * 3. ปรับสถานะ isPoweredOn เป็น false
     * @return true ถ้าปิดเครื่องสำเร็จ
     */
    @Override
    public boolean powerOff() {
        if (currentRunningApp != null) {
            closeCurrentApp();
        }
        this.display.turnOff();
        this.isPoweredOn = false;
        return true;
    }

    /**
     * สลับสถานะเปิด/ปิดเครื่อง (Toggle Power Key)
     */
    public void togglePower() {
        if (isPoweredOn) {
            powerOff();
        } else {
            powerOn();
        }
    }

    // ============================================================================
    // Application Management (การจัดการติดตั้งและเปิดแอปพลิเคชัน)
    // ============================================================================

    /**
     * ติดตั้งแอปพลิเคชันใหม่ลงในเครื่อง
     * 1. บันทึกแพ็กเกจแอปพลิเคชันลงใน Map (installedApps)
     * 2. จำลองการบันทึกไฟล์ APK ลงในหน่วยความจำแฟลช (Storage)
     * 3. ส่ง Notification แจ้งว่าติดตั้งแอปสำเร็จ
     * 
     * @param app อ็อบเจกต์แอปพลิเคชันที่ต้องการติดตั้ง
     * @return true ถ้าติดตั้งสำเร็จ, false ถ้ามีแอปนี้อยู่แล้ว
     */
    public synchronized boolean installApp(AndroidApplication app) {
        if (installedApps.containsKey(app.getAppId())) {
            return false;
        }
        installedApps.put(app.getAppId(), app);
        storage.saveFile(app.getAppId() + ".apk", app.getSizeMb(), "application/vnd.android.package-archive");
        os.getNotificationManager().postNotification(
            "App Installed",
            app.getAppName() + " (" + app.getVersion() + ") successfully installed.",
            "Package Manager"
        );
        return true;
    }

    /**
     * สั่งเปิดแอปพลิเคชัน (Launch App)
     * จำลอง Android Activity Lifecycle:
     * 1. หากมีแอปอื่นเปิดอยู่ ให้สั่ง onPause() และ onStop() แอปเดิม
     * 2. เรียก onCreate() -> onStart() -> onResume() สำหรับแอปเป้าหมาย
     * 3. นำแอปเป้าหมายขึ้นมาไว้เป็น Recent App อันดับแรก
     * 
     * @param appId รหัสประจำแอป เช่น "com.psru.lostfound"
     * @return true ถ้าเปิดแอปสำเร็จ
     */
    public synchronized boolean launchApp(String appId) {
        if (!isPoweredOn) return false;
        AndroidApplication targetApp = installedApps.get(appId);
        if (targetApp == null) return false;

        // สลับแอปเดิมออกจาก Foreground
        if (currentRunningApp != null && !currentRunningApp.getAppId().equals(appId)) {
            currentRunningApp.onPause();
            currentRunningApp.onStop();
        }

        // เริ่มต้น Lifecycle ของแอปใหม่
        targetApp.onCreate();
        targetApp.onStart();
        targetApp.onResume();
        this.currentRunningApp = targetApp;
        recentAppIds.remove(appId);
        recentAppIds.add(0, appId);
        return true;
    }

    /**
     * ปิดแอปพลิเคชันปัจจุบันที่กำลังแสดงผลอยู่
     * เรียก Lifecycle: onPause() -> onStop() -> onDestroy()
     */
    public synchronized void closeCurrentApp() {
        if (currentRunningApp != null) {
            currentRunningApp.onPause();
            currentRunningApp.onStop();
            currentRunningApp.onDestroy();
            currentRunningApp = null;
        }
    }

    /**
     * จำลองการกดปุ่ม Home: พักการทำงานของแอป (onPause -> onStop) และกลับสู่หน้าโฮม
     */
    public void pressHomeButton() {
        if (!isPoweredOn) return;
        if (currentRunningApp != null) {
            currentRunningApp.onPause();
            currentRunningApp.onStop();
            currentRunningApp = null;
        }
    }

    /**
     * จำลองการกดปุ่ม Back: ปิดแอปปัจจุบัน (closeCurrentApp)
     */
    public void pressBackButton() {
        if (!isPoweredOn) return;
        if (currentRunningApp != null) {
            closeCurrentApp();
        }
    }

    /**
     * [Lecture 4: Polymorphism - Dynamic Method Dispatch]
     * ส่งคำสั่ง (Action) ไปยังแอปพลิเคชันที่กำลังเปิดอยู่
     * เช่น "LOGIN", "CREATE_POST", "SET_BRIGHTNESS"
     * 
     * @param action ชื่อการกระทำ
     * @param params พารามิเตอร์ของคำสั่งในรูปแบบ Map
     * @return ผลลัพธ์ ActionResult
     */
    public AndroidApplication.ActionResult executeAppAction(String action, Map<String, Object> params) {
        if (!isPoweredOn) {
            return new AndroidApplication.ActionResult(false, "Device is powered off", null);
        }
        if (currentRunningApp == null) {
            return new AndroidApplication.ActionResult(false, "No app currently running", null);
        }
        return currentRunningApp.executeAction(action, params);
    }

    // ============================================================================
    // Getter Methods
    // ============================================================================

    /**
     * อ่านอ็อบเจกต์ระบบปฏิบัติการ AndroidOS
     * @return AndroidOS
     */
    public AndroidOS getOs() {
        return os;
    }

    /**
     * อ่านตารางแอปพลิเคชันที่ติดตั้งทั้งหมด
     * @return Map<appId, AndroidApplication>
     */
    public Map<String, AndroidApplication> getInstalledApps() {
        return installedApps;
    }

    /**
     * อ่านแอปพลิเคชันที่กำลังเปิดทำงานอยู่บนหน้าจอ
     * @return AndroidApplication หรือ null ถ้าอยู่ที่หน้าโฮม
     */
    public AndroidApplication getCurrentRunningApp() {
        return currentRunningApp;
    }
}
