package com.android.events;

import com.android.model.StudentUser;

/**
 * ============================================================================
 * [Lecture 5.1: Event-driven Programming & Observer Pattern]
 * ============================================================================
 * 
 * อินเทอร์เฟซสำหรับรับฟังเหตุการณ์ (Event Listener) เกี่ยวกับการยืนยันตัวตน
 * ทำหน้าที่เป็น Observer ที่คอยดักรับ Callback เมื่อเกิดการเปลี่ยนแปลงสถานะใน AuthController
 * 
 * @author CPE Student
 * @version 1.0
 */
public interface AuthEventListener {

    /**
     * เรียกใช้งานเมื่อผู้ใช้งานเข้าสู่ระบบสำเร็จ
     * @param user ข้อมูลอ็อบเจกต์ผู้ใช้งานที่ล็อกอินเข้ามา
     */
    void onLoginSuccess(StudentUser user);

    /**
     * เรียกใช้งานเมื่อมีการลงทะเบียนสมาชิกใหม่สำเร็จ
     * @param user ข้อมูลอ็อบเจกต์ผู้ใช้งานที่เพิ่งสมัครเข้ามาใหม่
     */
    void onRegisterSuccess(StudentUser user);

    /**
     * เรียกใช้งานเมื่อผู้ใช้งานออกจากระบบ
     */
    void onLogout();

    /**
     * เรียกใช้งานเมื่อการยืนยันตัวตนล้มเหลว (เช่น รหัสผ่านผิด, กรอกข้อมูลไม่ครบ)
     * @param errorMessage ข้อความแจ้งสาเหตุความล้มเหลว
     */
    void onAuthFailed(String errorMessage);
}
