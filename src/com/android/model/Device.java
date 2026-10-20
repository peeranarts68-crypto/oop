package com.android.model;

/**
 * ============================================================================
 * [Lecture 4: OOP Concepts - Abstraction (การสร้างสิ่งนามธรรม)]
 * ============================================================================
 * 
 * คลาสนามธรรม (Abstract Class) แสดงคุณลักษณะและพฤติกรรมพื้นฐานร่วมกันของอุปกรณ์อิเล็กทรอนิกส์ทุกชนิด (Device)
 * 
 * คุณสมบัติสำคัญของ Abstract Class:
 * 1. ไม่สามารถสร้าง Instance โดยตรงได้ (ไม่สามารถเขียน new Device() ได้)
 * 2. ทำหน้าที่เป็นแม่แบบ (Blueprint) ให้คลาสลูกสืบทอด (Inherit) ไปใช้งาน
 * 3. มี Abstract Method เพื่อบังคับให้คลาสลูกต้องกำหนดพฤติกรรมที่แน่นอนเอง (Polymorphism)
 * 
 * @author CPE Student
 * @version 1.0
 */
public abstract class Device {

    // --- คุณลักษณะประจำอุปกรณ์ (Attributes / Encapsulation) ---
    
    /** ยี่ห้อของอุปกรณ์ เช่น Google, Samsung, Apple */
    private final String brand;

    /** รุ่นของอุปกรณ์ เช่น Pixel 9 Pro, Galaxy S24 */
    private final String model;

    /** หมายเลขประจำเครื่องที่ไม่ซ้ำกัน (Serial Number) */
    private final String serialNumber;

    /** 
     * สถานะการเปิด/ปิดเครื่อง (true = เปิดอยู่, false = ปิดอยู่) 
     * ใช้ protected เพื่อให้คลาสลูก (เช่น SmartPhone, AndroidPhone) สามารถเข้าถึงได้โดยตรง
     */
    protected boolean isPoweredOn;

    /**
     * [Lecture 3: Constructor]
     * ตัวสร้างอ็อบเจกต์ Device โดยกำหนดค่าเริ่มต้นของฮาร์ดแวร์
     * 
     * @param brand ยี่ห้อของอุปกรณ์
     * @param model รุ่นของอุปกรณ์
     * @param serialNumber หมายเลขประจำเครื่อง
     */
    public Device(String brand, String model, String serialNumber) {
        this.brand = brand;
        this.model = model;
        this.serialNumber = serialNumber;
        this.isPoweredOn = false; // เมื่อเริ่มสร้าง อุปกรณ์จะอยู่ในสถานะปิดเครื่องเสมอ
    }

    // ============================================================================
    // Getter Methods (Encapsulation: เปิดให้ภายนอกอ่านค่าได้อย่างปลอดภัย)
    // ============================================================================

    /**
     * อ่านชื่อยี่ห้อของอุปกรณ์
     * @return ยี่ห้อ (Brand)
     */
    public String getBrand() {
        return brand;
    }

    /**
     * อ่านชื่อรุ่นของอุปกรณ์
     * @return ชื่อรุ่น (Model)
     */
    public String getModel() {
        return model;
    }

    /**
     * อ่านหมายเลขประจำเครื่อง
     * @return Serial Number
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * ตรวจสอบว่าเครื่องเปิดทำงานอยู่หรือไม่
     * @return true ถ้าเครื่องเปิดอยู่, false ถ้าเครื่องปิดอยู่
     */
    public boolean isPoweredOn() {
        return isPoweredOn;
    }

    // ============================================================================
    // Abstract Methods (บังคับให้คลาสลูกต้อง Override และกำหนดขั้นตอนการทำงานเอง)
    // ============================================================================

    /**
     * คำสั่งเปิดเครื่อง (Power On)
     * แต่ละชนิดของอุปกรณ์มีขั้นตอนเปิดเครื่องต่างกัน เช่น ตรวจสอบแบตเตอรี่, แสดงโลโก้
     * @return true ถ้าเปิดเครื่องสำเร็จ, false ถ้าเปิดไม่สำเร็จ (เช่น แบตเตอรี่หมด)
     */
    public abstract boolean powerOn();

    /**
     * คำสั่งปิดเครื่อง (Power Off)
     * คลาสลูกต้องจัดการบันทึกข้อมูล ปิดหน้าจอ และเคลียร์แอปก่อนปิดเครื่อง
     * @return true ถ้าปิดเครื่องสำเร็จ
     */
    public abstract boolean powerOff();

    /**
     * คืนค่าระบุประเภทอุปกรณ์ เช่น "SmartPhone", "Tablet", "Laptop"
     * @return สตริงประเภทอุปกรณ์
     */
    public abstract String getDeviceType();
}
