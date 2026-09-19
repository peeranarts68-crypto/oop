package com.android.events;

import com.android.model.StudentUser;

/**
 * [Lecture 5.1: Event-driven Programming]
 *
 * อินเทอร์เฟซสำหรับรับฟังเหตุการณ์ (Event Listener) เกี่ยวกับการยืนยันตัวตน
 * เมื่อเกิดเหตุการณ์ Login หรือ Register สำเร็จ ระบบจะเรียก Callback เหล่านี้
 */
public interface AuthEventListener {
    void onLoginSuccess(StudentUser user);
    void onRegisterSuccess(StudentUser user);
    void onLogout();
    void onAuthFailed(String errorMessage);
}
