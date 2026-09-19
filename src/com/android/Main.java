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
 * ออกแบบตามหลักสูตรรายวิชา (Lecture 0 - 6):
 *  - Lecture 2: Coding in Java (Syntax, Control Flow)
 *  - Lecture 3: OOP Class and Object (Device, Battery, Display, StudentUser)
 *  - Lecture 4: OOP Concepts (Encapsulation, Inheritance, Polymorphism, Abstraction)
 *  - Lecture 5.1: Event-driven Programming (PhoneEventListener, AuthEventListener)
 *  - Lecture 5.2: MVC Architectural Pattern (Model, View, Controller)
 *  - Lecture 6: Multitasking (PhoneBackgroundService running on Thread)
 * ============================================================================
 */
public class Main {
    public static final int PORT = 8088;

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   Android Smartphone OOP Architecture Simulator    ");
        System.out.println("   [Aligned with Syllabus: Lectures 0 - 6]          ");
        System.out.println("====================================================");

        // -------------------------------------------------------------
        // 1. [Lecture 5.2: MODEL] สร้าง Model ของสมาร์ตโฟน
        // -------------------------------------------------------------
        AndroidPhone pixelPhone = new AndroidPhone(
            "Google",
            "Pixel 9 Pro (OOP Edition)",
            "PX9-OOP-2026-TH",
            5050,         // 5050 mAh battery
            "1280 x 2856", // OLED resolution
            6.7,          // 6.7 inches screen
            131072,       // 128 GB storage (in MB)
            50,           // 50 MP camera
            "Android 15 (Vanilla Ice Cream)",
            35            // API Level 35
        );

        // -------------------------------------------------------------
        // 2. [Lecture 5.2: CONTROLLER] สร้าง Controllers
        // -------------------------------------------------------------
        PhoneController phoneController = new PhoneController(pixelPhone);
        AuthController authController = new AuthController();

        // -------------------------------------------------------------
        // 3. [Lecture 5.1: EVENT-DRIVEN PROGRAMMING] ลงทะเบียน Event Listeners
        // -------------------------------------------------------------
        phoneController.addListener(new PhoneEventListener() {
            @Override
            public void onPowerChanged(boolean isPoweredOn) {
                System.out.println("[EVENT] Phone Power Changed: " + (isPoweredOn ? "ON" : "OFF"));
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

        // ติดตั้งแอปพลิเคชันเข้าสู่ Model
        pixelPhone.installApp(new SettingsApp(pixelPhone));
        pixelPhone.installApp(new PsruLostFoundApp(pixelPhone, authController));

        // -------------------------------------------------------------
        // 4. [Lecture 6: MULTITASKING] รัน Background Service บน Thread แยก
        // -------------------------------------------------------------
        PhoneBackgroundService backgroundService = new PhoneBackgroundService(pixelPhone);
        Thread backgroundThread = new Thread(backgroundService, "PhoneBackgroundService-Thread");
        backgroundThread.setDaemon(true);
        backgroundThread.start();
        System.out.println("[MULTITASKING] Background Thread launched successfully.");

        // -------------------------------------------------------------
        // 5. [Lecture 5.2: VIEW] เริ่มต้นเว็บเซิร์ฟเวอร์เปิดใช้งาน Web GUI
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
