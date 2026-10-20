package com.android;

import com.android.controller.AuthController;
import com.android.controller.PhoneController;
import com.android.events.AuthEventListener;
import com.android.events.PhoneEventListener;
import com.android.model.AndroidPhone;
import com.android.model.StudentUser;
import com.android.model.apps.PsruLostFoundApp;
import com.android.model.apps.SettingsApp;
import com.android.multitasking.PhoneBackgroundService;
import com.android.server.AndroidHttpServer;

import java.io.File;

/**
 * ============================================================================
 * Application Entry Point - โครงงาน Android OOP Simulator
 * จุดเริ่มต้นการทำงานของโปรแกรมหลัก (Main Method)
 * ============================================================================
 * 
 * โครงงานนี้ออกแบบตามหลักสูตรรายวิชา Object-Oriented Programming (Lecture 0 - 6):
 * 
 * - Lecture 2: Coding in Java
 *   - โครงสร้างไวยากรณ์ Java, Control Flow (if-else, switch, loop), Data Types
 *   - การตรวจสอบความถูกต้องของข้อมูล (Validation) ใน AuthController
 * 
 * - Lecture 3: OOP Class and Object
 *   - การสร้าง Class และ Object: Device, Battery, Display, Storage, Camera, StudentUser
 *   - ตัวแปร Attribute, เมธอด Method, และการสร้าง Constructor
 * 
 * - Lecture 4: OOP Concepts (เสาหลัก OOP ทั้ง 4 ประการ)
 *   - Encapsulation: ซ่อนตัวแปรเป็น private เข้าถึงผ่าน Getter/Setter
 *   - Abstraction: คลาสนามธรรม Device และ Interface NetworkAdapter
 *   - Inheritance: การสืบทอด Device -> SmartPhone -> AndroidPhone
 *   - Polymorphism: Dynamic Method Dispatch ใน executeAction() และ Method Overriding/Overloading
 * 
 * - Lecture 5.1: Event-driven Programming
 *   - Observer Pattern: PhoneEventListener และ AuthEventListener รับฟังสัญญาณ Event
 * 
 * - Lecture 5.2: MVC Architectural Pattern
 *   - Model: com.android.model (จัดการข้อมูลและตรรกะระบบ)
 *   - View: web/ (HTML/CSS/JavaScript แสดงผลบนเว็บเบราว์เซอร์)
 *   - Controller: com.android.controller (ควบคุมการทำงานระหว่าง View และ Model)
 * 
 * - Lecture 6: Multitasking
 *   - PhoneBackgroundService รันบน Thread แยกต่างหาก (Background Thread) ผ่าน Runnable
 * 
 * ============================================================================
 * @author CPE Student
 * @version 1.0
 */
public class Main {

    /** พอร์ตเริ่มต้นสำหรับการรันเว็บเซิร์ฟเวอร์จำลอง (http://localhost:8088) */
    public static final int PORT = 8088;

    /**
     * เมธอดหลักจุดเริ่มต้นของโปรแกรม
     * @param args อาร์กิวเมนต์จาก Command Line
     */
    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   Android Smartphone OOP Architecture Simulator    ");
        System.out.println("   [Aligned with Syllabus: Lectures 0 - 6]          ");
        System.out.println("====================================================");

        // -------------------------------------------------------------
        // ขั้นตอนที่ 1. [Lecture 5.2: MODEL] สร้าง Model ของสมาร์ตโฟน
        // -------------------------------------------------------------
        // จำลองเครื่อง Google Pixel 9 Pro (OOP Edition) พร้อมสเปกฮาร์ดแวร์จริง
        AndroidPhone pixelPhone = new AndroidPhone(
            "Google",
            "Pixel 9 Pro (OOP Edition)",
            "PX9-OOP-2026-TH",
            5050,          // แบตเตอรี่ความจุ 5050 mAh
            "1280 x 2856",  // หน้าจอความละเอียด OLED 1280 x 2856
            6.7,           // ขนาดหน้าจอ 6.7 นิ้ว
            131072,        // พื้นที่เก็บข้อมูล 128 GB (แปลงเป็น MB = 131,072 MB)
            50,            // กล้องหลัง 50 MP
            "Android 15 (Vanilla Ice Cream)", // เวอร์ชัน OS
            35             // API Level 35
        );

        // -------------------------------------------------------------
        // ขั้นตอนที่ 2. [Lecture 5.2: CONTROLLER] สร้าง Controllers
        // -------------------------------------------------------------
        PhoneController phoneController = new PhoneController(pixelPhone);
        AuthController authController = new AuthController();

        // -------------------------------------------------------------
        // ขั้นตอนที่ 3. [Lecture 5.1: EVENT-DRIVEN PROGRAMMING] ลงทะเบียน Event Listeners
        // -------------------------------------------------------------
        // ลงทะเบียนผู้รับฟังเหตุการณ์โทรศัพท์ (ฮาร์ดแวร์ Power, Volume, App Launch/Close)
        phoneController.addListener(new PhoneEventListener() {
            @Override
            public void onPowerChanged(boolean isPoweredOn) {
                System.out.println("[EVENT] Phone Power Changed: " + (isPoweredOn ? "ON (เปิดเครื่อง)" : "OFF (ปิดเครื่อง)"));
            }

            @Override
            public void onVolumeChanged(int newVolume) {
                System.out.println("[EVENT] Phone Volume Changed to: " + newVolume + "%");
            }

            @Override
            public void onAppLaunched(String appName) {
                System.out.println("[EVENT] App Launched: " + appName);
            }

            @Override
            public void onAppClosed(String appName) {
                System.out.println("[EVENT] App Closed: " + appName);
            }
        });

        // ลงทะเบียนผู้รับฟังเหตุการณ์ระบบยืนยันตัวตน (Login, Register, Logout)
        authController.addListener(new AuthEventListener() {
            @Override
            public void onLoginSuccess(StudentUser user) {
                System.out.println("[EVENT] Login Success: " + user.getFullName() + " (" + user.getFaculty() + ")");
            }

            @Override
            public void onRegisterSuccess(StudentUser user) {
                System.out.println("[EVENT] New Student Registered: " + user.getFullName() + " (" + user.getFaculty() + ")");
            }

            @Override
            public void onLogout() {
                System.out.println("[EVENT] User Logged Out.");
            }

            @Override
            public void onAuthFailed(String errorMessage) {
                System.out.println("[EVENT] Auth Failed: " + errorMessage);
            }
        });

        // ติดตั้งแอปพลิเคชันพื้นฐานเข้าสู่โทรศัพท์ (Model)
        pixelPhone.installApp(new SettingsApp(pixelPhone));
        pixelPhone.installApp(new PsruLostFoundApp(pixelPhone, authController));

        // -------------------------------------------------------------
        // ขั้นตอนที่ 4. [Lecture 6: MULTITASKING] รัน Background Service บน Thread แยก
        // -------------------------------------------------------------
        // สร้าง Background Thread สำหรับเซอร์วิสเบื้องหลัง โดยไม่ทำให้ Main Thread บล็อก
        PhoneBackgroundService backgroundService = new PhoneBackgroundService(pixelPhone);
        Thread backgroundThread = new Thread(backgroundService, "PhoneBackgroundService-Thread");
        backgroundThread.setDaemon(true); // ตั้งเป็น Daemon Thread เพื่อให้ปิดพร้อมกับ JVM ได้
        backgroundThread.start();
        System.out.println("[MULTITASKING] Background Thread launched successfully.");

        // -------------------------------------------------------------
        // ขั้นตอนที่ 5. [Lecture 5.2: VIEW] เริ่มต้นเว็บเซิร์ฟเวอร์เปิดใช้งาน Web GUI
        // -------------------------------------------------------------
        File webDir = new File("web");
        if (!webDir.exists()) {
            webDir = new File("../web");
        }

        try {
            AndroidHttpServer server = new AndroidHttpServer(PORT, phoneController, webDir);
            server.start();

            System.out.println("[SERVER] Web GUI & REST API active at http://localhost:" + PORT);
            System.out.println("[INFO] Ready! Press Ctrl+C in terminal to stop server.");

            // ลงทะเบียน Shutdown Hook เพื่อเคลียร์ทรัพยากรเมื่อเซิร์ฟเวอร์ปิดตัว
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SERVER] Shutting down Android OOP Simulator...");
                backgroundService.stopService();
                server.stop();
            }));

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
