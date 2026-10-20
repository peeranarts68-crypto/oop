package com.android.model.hardware;

/**
 * [Lecture 3: OOP Class and Object]
 * [Lecture 4: OOP Concepts - Encapsulation]
 *
 * คลาสชิ้นส่วนฮาร์ดแวร์หน้าจอแสดงผล (Display Hardware Component) ของสมาร์ตโฟน
 * จัดการข้อมูลความละเอียดหน้าจอ, ขนาดเส้นทแยงมุม, อัตรารีเฟรชเรท (Hz), ระดับความสว่าง, และสถานะเปิด/ปิดหน้าจอ
 */
public class Display {
    private final String resolution;       // ความละเอียดหน้าจอ เช่น "1080x2400 FHD+"
    private final double diagonalInches;   // ขนาดหน้าจอตามแนวทแยง (นิ้ว) เช่น 6.7 นิ้ว
    private int brightness;                // ระดับความสว่างของหน้าจอ (0 - 100%)
    private boolean isScreenOn;            // สถานะหน้าจอ (true = หน้าจอเปิดติดอยู่, false = ดับจอ)
    private int refreshRateHz;             // อัตรารีเฟรชเรทของหน้าจอ (Hz) เช่น 60, 90, 120 Hz

    /**
     * คอนสตรักเตอร์กำหนดคุณสมบัติหน้าจอเริ่มต้น
     */
    public Display(String resolution, double diagonalInches, int refreshRateHz) {
        this.resolution = resolution;
        this.diagonalInches = diagonalInches;
        this.brightness = 75;              // ความสว่างเริ่มต้น 75%
        this.isScreenOn = true;            // เปิดหน้าจอเป็นค่าเริ่มต้น
        this.refreshRateHz = refreshRateHz;
    }

    // --- Getter & Setter Methods (Encapsulation) ---

    public String getResolution() {
        return resolution;
    }

    public double getDiagonalInches() {
        return diagonalInches;
    }

    public int getBrightness() {
        return brightness;
    }

    /**
     * กำหนดระดับความสว่างหน้าจอ พร้อมระบบตรวจสอบให้อยู่ในช่วง 0 - 100%
     */
    public void setBrightness(int brightness) {
        this.brightness = Math.max(0, Math.min(100, brightness));
    }

    public boolean isScreenOn() {
        return isScreenOn;
    }

    public void turnOn() {
        this.isScreenOn = true;
    }

    public void turnOff() {
        this.isScreenOn = false;
    }

    public void toggleScreen() {
        this.isScreenOn = !this.isScreenOn;
    }

    public int getRefreshRateHz() {
        return refreshRateHz;
    }

    public void setRefreshRateHz(int refreshRateHz) {
        this.refreshRateHz = refreshRateHz;
    }
}
