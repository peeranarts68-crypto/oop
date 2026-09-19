package com.android.model;

/**
 * [Lecture 4: OOP Concepts - Abstraction]
 *
 * คลาสนามธรรม (Abstract Class) แสดงแนวคิด Abstraction ขั้นพื้นฐานของอุปกรณ์อิเล็กทรอนิกส์
 * ไม่สามารถสร้างเป็น Instance ได้โดยตรง (new Device() ไม่ได้)
 * บังคับให้คลาสลูกต้อง implement เมธอด powerOn() และ powerOff() (Polymorphism)
 */
public abstract class Device {
    private final String brand;
    private final String model;
    private final String serialNumber;
    protected boolean isPoweredOn;

    public Device(String brand, String model, String serialNumber) {
        this.brand = brand;
        this.model = model;
        this.serialNumber = serialNumber;
        this.isPoweredOn = false;
    }

    // --- Getter Methods (Encapsulation) ---
    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public boolean isPoweredOn() {
        return isPoweredOn;
    }

    // Abstract methods ให้คลาสลูกไปกำหนดพฤติกรรมเอง (Polymorphism)
    public abstract boolean powerOn();
    public abstract boolean powerOff();
    public abstract String getDeviceType();
}
