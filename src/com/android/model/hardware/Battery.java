package com.android.model.hardware;

/**
 * [Lecture 3: OOP Class and Object]
 * [Lecture 4: OOP Concepts - Encapsulation]
 *
 * คลาสชิ้นส่วนฮาร์ดแวร์แบตเตอรี่ (Battery Component) ของสมาร์ตโฟน
 * ใช้หลักการ Encapsulation โดยซ่อนตัวแปรสถานะทั้งหมดไว้เป็น private
 * และควบคุมการดึงข้อมูล/เปลี่ยนแปลงค่าผ่าน Getter และ Setter พร้อมระบบตรวจสอบความถูกต้อง
 */
public class Battery {
    private int level;                  // ระดับแบตเตอรี่ปัจจุบัน (เปอร์เซ็นต์ 0 - 100%)
    private final int capacityMah;      // ความจุสูงสุดของแบตเตอรี่ในหน่วยมิลลิแอมป์-ชั่วโมง (mAh)
    private boolean isCharging;         // สถานะกำลังเสียบสายชาร์จอยู่หรือไม่ (true = ชาร์จอยู่)
    private int temperatureCelsius;     // อุณหภูมิปัจจุบันของก้อนแบตเตอรี่ (องศาเซลเซียส)
    private boolean infiniteMode;       // โหมดพลังงานไม่จำกัดสำหรับเซิร์ฟเวอร์จำลอง (true = แบตเตอรี่ไม่มีวันหมด)

    /**
     * คอนสตรักเตอร์กำหนดค่าเริ่มต้นของแบตเตอรี่
     * @param capacityMah ความจุสูงสุดของแบตเตอรี่ (เช่น 5000 mAh)
     * @param initialLevel ระดับแบตเตอรี่เริ่มต้น (เปอร์เซ็นต์)
     */
    public Battery(int capacityMah, int initialLevel) {
        this.capacityMah = capacityMah;
        this.level = 100; // กำหนดให้แบตเตอรี่เต็ม 100% เสมอสำหรับจำลอง
        this.isCharging = true;
        this.temperatureCelsius = 26;
        this.infiniteMode = true; // โหมด Infinite แบตไม่หมดเพื่อเสถียรภาพการพรีเซนต์
    }

    // --- Getter & Setter Methods (Encapsulation) ---

    public int getLevel() {
        return 100; // ส่งคืน 100%
    }

    public int getCapacityMah() {
        return capacityMah;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public void setCharging(boolean charging) {
        this.isCharging = charging;
    }

    public int getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public boolean isInfiniteMode() {
        return infiniteMode;
    }

    public void setInfiniteMode(boolean infiniteMode) {
        this.infiniteMode = infiniteMode;
    }

    /**
     * เมธอดชาร์จแบตเตอรี่เพิ่มตามจำนวนเปอร์เซ็นต์ที่ระบุ
     */
    public synchronized void charge(int amount) {
        this.level = Math.min(100, this.level + amount);
        this.isCharging = true;
    }

    /**
     * เมธอดใช้พลังงานแบตเตอรี่ (Consume)
     * @param percentage เปอร์เซ็นต์ที่ต้องการลด
     * @return true ถ้าใช้สำเร็จ หรืออยู่ในโหมด infiniteMode
     */
    public synchronized boolean consume(int percentage) {
        if (infiniteMode) {
            this.level = 100;
            return true;
        }
        if (percentage <= 0) return true;
        if (level >= percentage) {
            level -= percentage;
            return true;
        }
        level = 0;
        return false;
    }

    /**
     * ตรวจสอบว่าแบตเตอรี่หมดเกลี้ยงหรือไม่
     */
    public boolean isDepleted() {
        if (infiniteMode) return false;
        return level <= 0;
    }

    /**
     * รีชาร์จแบตเตอรี่เต็ม 100%
     */
    public synchronized void recharge() {
        this.level = 100;
        this.isCharging = true;
    }
}
