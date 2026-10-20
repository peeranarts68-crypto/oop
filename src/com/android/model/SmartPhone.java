package com.android.model;

import com.android.model.hardware.Battery;
import com.android.model.hardware.Camera;
import com.android.model.hardware.DefaultNetworkAdapter;
import com.android.model.hardware.Display;
import com.android.model.hardware.NetworkAdapter;
import com.android.model.hardware.Storage;

/**
 * ============================================================================
 * [Lecture 4: OOP Concepts - Inheritance (การสืบทอด) & Composition (การประกอบ)]
 * ============================================================================
 * 
 * คลาสนามธรรมของสมาร์ตโฟน (SmartPhone)
 * 1. สืบทอดคุณลักษณะพื้นฐานมาจาก Device ด้วยคีย์เวิร์ด `extends Device` (Inheritance)
 *    ทำให้ได้รับ brand, model, serialNumber และ isPoweredOn มาโดยอัตโนมัติ
 * 2. ประกอบด้วย (Composition - Has-A Relationship) ชิ้นส่วนฮาร์ดแวร์จริง:
 *    - แบตเตอรี่ (Battery)
 *    - จอแสดงผล (Display)
 *    - หน่วยความจำแฟลช (Storage)
 *    - กล้องดิจิทัล (Camera)
 *    - โมดูลเครือข่ายอินเทอร์เน็ต (NetworkAdapter)
 * 
 * @author CPE Student
 * @version 1.0
 */
public abstract class SmartPhone extends Device {

    // --- ชิ้นส่วนฮาร์ดแวร์ที่ประกอบเป็นโทรศัพท์ (Composition / Encapsulation) ---
    
    /** ก้อนแบตเตอรี่ของโทรศัพท์ */
    protected final Battery battery;

    /** แผงหน้าจอแสดงผล */
    protected final Display display;

    /** หน่วยความจำจัดเก็บข้อมูลภายในเครื่อง */
    protected final Storage storage;

    /** กล้องดิจิทัลสำหรับถ่ายรูป */
    protected final Camera camera;

    /** โมดูลการเชื่อมต่อเครือข่าย (Wi-Fi / 5G Cellular) ผ่าน Interface */
    protected final NetworkAdapter networkAdapter;

    /** ระดับเสียงของลำโพง (ช่วง 0 - 100%) */
    protected int volume;

    /**
     * [Lecture 3: Constructor] & [Lecture 4: super()]
     * ตัวสร้างสมาร์ตโฟน พร้อมประกอบฮาร์ดแวร์ทั้งหมดเข้าด้วยกัน
     * 
     * @param brand ยี่ห้อของโทรศัพท์ เช่น Google
     * @param model ชื่อรุ่นโทรศัพท์ เช่น Pixel 9 Pro
     * @param serialNumber หมายเลขประจำเครื่อง
     * @param batteryMah ความจุแบตเตอรี่ (มิลลิแอมป์-ชั่วโมง)
     * @param screenResolution ความละเอียดหน้าจอ เช่น "1280 x 2856"
     * @param screenInches ขนาดหน้าจอแนวทแยง (นิ้ว) เช่น 6.7
     * @param storageMb ความจุพื้นที่จัดเก็บ (เมกะไบต์)
     * @param cameraMp ความละเอียดกล้องหลัก (ล้านพิกเซล)
     */
    public SmartPhone(String brand, String model, String serialNumber,
                      int batteryMah, String screenResolution, double screenInches,
                      int storageMb, int cameraMp) {
        // [Lecture 4] เรียก Constructor ของคลาสแม่ (Device) เพื่อกำหนด brand, model, serialNumber
        super(brand, model, serialNumber);

        // [Lecture 4: Composition] สร้างและประกอบชิ้นส่วนฮาร์ดแวร์ภายใน
        this.battery = new Battery(batteryMah, 100);
        this.display = new Display(screenResolution, screenInches, 120);
        this.storage = new Storage(storageMb);
        this.camera = new Camera(cameraMp);
        this.networkAdapter = new DefaultNetworkAdapter();
        this.volume = 70; // ระดับเสียงเริ่มต้นที่ 70%
    }

    // ============================================================================
    // Getter Methods สำหรับฮาร์ดแวร์แต่ละชิ้น
    // ============================================================================

    /**
     * อ่านอ็อบเจกต์แบตเตอรี่
     * @return Battery
     */
    public Battery getBattery() {
        return battery;
    }

    /**
     * อ่านอ็อบเจกต์จอแสดงผล
     * @return Display
     */
    public Display getDisplay() {
        return display;
    }

    /**
     * อ่านอ็อบเจกต์หน่วยความจำ
     * @return Storage
     */
    public Storage getStorage() {
        return storage;
    }

    /**
     * อ่านอ็อบเจกต์กล้อง
     * @return Camera
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * อ่านอ็อบเจกต์โมดูลเครือข่าย
     * @return NetworkAdapter
     */
    public NetworkAdapter getNetworkAdapter() {
        return networkAdapter;
    }

    /**
     * อ่านระดับเสียงปัจจุบัน (0 - 100)
     * @return ระดับเสียง
     */
    public int getVolume() {
        return volume;
    }

    /**
     * ปรับตั้งระดับเสียง (พร้อม Validation ตรวจสอบไม่ให้ต่ำกว่า 0 หรือเกิน 100)
     * @param volume ระดับเสียงเป้าหมาย
     */
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
    }

    /**
     * เพิ่มระดับเสียงขึ้น 10 หน่วย
     */
    public void volumeUp() {
        setVolume(this.volume + 10);
    }

    /**
     * ลดระดับเสียงลง 10 หน่วย
     */
    public void volumeDown() {
        setVolume(this.volume - 10);
    }

    /**
     * [Lecture 4: Polymorphism - Method Overriding]
     * กำหนดชนิดอุปกรณ์เป็น "SmartPhone"
     * @return สตริงประเภทอุปกรณ์
     */
    @Override
    public String getDeviceType() {
        return "SmartPhone";
    }
}
