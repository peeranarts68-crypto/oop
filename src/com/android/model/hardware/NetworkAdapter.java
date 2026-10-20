package com.android.model.hardware;

/**
 * [Lecture 4: OOP Concepts - Abstraction & Interface]
 *
 * อินเทอร์เฟซตัวแปลงสัญญาณเครือข่ายฮาร์ดแวร์ (Hardware Network Adapter Interface)
 * ทำหน้าที่เป็น Abstraction Layer กำหนดข้อกำหนด (Contract) ในการเชื่อมต่ออินเทอร์เน็ต
 * สำหรับรองรับทั้ง Wi-Fi และเครือข่ายเซลลูลาร์มือถือ (Cellular Data)
 */
public interface NetworkAdapter {
    /** ตรวจสอบว่ามีการเชื่อมต่ออินเทอร์เน็ตหรือไม่ */
    boolean isConnected();

    /** ส่งคืนประเภทการเชื่อมต่อ เช่น "Wi-Fi (5GHz)" หรือ "5G Mobile" */
    String getConnectionType();

    /** ส่งคืนความแรงสัญญาณเครือข่าย (0 - 100%) */
    int getSignalStrength();

    /** สลับสถานะเปิด/ปิด Wi-Fi */
    void toggleWifi();

    /** สลับสถานะเปิด/ปิด Cellular */
    void toggleCellular();

    /** ตรวจสอบว่า Wi-Fi เปิดอยู่หรือไม่ */
    boolean isWifiEnabled();

    /** ตรวจสอบว่า Cellular เปิดอยู่หรือไม่ */
    boolean isCellularEnabled();
}
