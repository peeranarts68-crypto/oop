package com.android.controller;

import com.android.events.AuthEventListener;
import com.android.model.DataStorage;
import com.android.model.StudentUser;
import com.android.model.apps.AndroidApplication.ActionResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [Lecture 5.2: MVC Architectural Pattern - Controller]
 *
 * คลาส Controller จัดการการตรวจสอบความถูกต้องของการเข้าสู่ระบบ
 * การสมัครสมาชิก และการอัปเดตรูปภาพโปรไฟล์ / ข้อมูลติดต่อ Instagram (IG)
 * โหลดและบันทึกเฉพาะข้อมูลจริงจากผู้ใช้ผ่าน DataStorage (ไม่มีข้อมูล Mock สุ่มขึ้นมาเอง)
 */
public class AuthController {
    private final Map<String, StudentUser> userDatabase;
    private StudentUser currentUser;
    private final List<AuthEventListener> listeners;

    public AuthController() {
        // โหลดข้อมูลเฉพาะที่ผู้ใช้งานสมัครสมาชิกไว้จริง
        this.userDatabase = new LinkedHashMap<>(DataStorage.loadUsers());

        // รับประกันว่าบัญชี 6812247050 (นายพีรนาท สิงห์โต) ได้รับสิทธิ์ ADMIN อัตโนมัติเสมอ
        StudentUser admin = userDatabase.get("6812247050");
        if (admin != null) {
            admin.setRole("ADMIN");
        } else {
            StudentUser newAdmin = new StudentUser(
                "6812247050",
                "พีรนาท สิงห์โต",
                "คณะวิศวกรรมศาสตร์และเทคโนโลยีอุตสาหกรรม",
                "@s.peeranart",
                "uploads/profile_user_1789807298340_a924f1.jpg",
                "123456",
                "ADMIN"
            );
            userDatabase.put("6812247050", newAdmin);
            DataStorage.saveUsers(userDatabase);
        }

        this.currentUser = null;
        this.listeners = new ArrayList<>();
    }

    public void addListener(AuthEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // [Lecture 2: Logic] เมธอด Login
    public ActionResult login(String studentId, String password) {
        if (studentId == null || studentId.trim().isEmpty() || password == null || password.isEmpty()) {
            String msg = "กรุณากรอกรหัสนักศึกษาและรหัสผ่าน";
            notifyAuthFailed(msg);
            return new ActionResult(false, msg, null);
        }

        studentId = studentId.trim();
        StudentUser user = userDatabase.get(studentId);
        if (user == null || !user.checkPassword(password)) {
            String msg = "รหัสนักศึกษาหรือรหัสผ่านไม่ถูกต้อง (หากยังไม่มีบัญชี กรุณาสมัครสมาชิก)";
            notifyAuthFailed(msg);
            return new ActionResult(false, msg, null);
        }

        this.currentUser = user;
        for (AuthEventListener l : listeners) {
            l.onLoginSuccess(user);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentId", user.getStudentId());
        data.put("fullName", user.getFullName());
        data.put("faculty", user.getFaculty());
        data.put("instagram", user.getInstagram());
        data.put("phone", user.getInstagram());
        data.put("line", user.getLine());
        data.put("profileImage", user.getProfileImage());
        data.put("role", user.getRole());
        data.put("isAdmin", user.isAdmin());
        return new ActionResult(true, "เข้าสู่ระบบสำเร็จ", data);
    }

    // [Lecture 2: Validation] เมธอด Register พร้อมบันทึก Instagram และรูปโปรไฟล์
    public ActionResult register(String studentId, String fullName, String faculty, String instagram, String line, String profileImage, String password) {
        studentId = (studentId != null) ? studentId.trim() : "";
        fullName = (fullName != null) ? fullName.trim() : "";
        faculty = (faculty != null) ? faculty.trim() : "";
        instagram = (instagram != null) ? instagram.trim() : "";
        line = (line != null) ? line.trim() : "";
        profileImage = (profileImage != null) ? profileImage.trim() : "";
        password = (password != null) ? password : "";

        if (studentId.length() < 10) {
            String msg = "รหัสนักศึกษาต้องมีความยาว 10 หลัก";
            notifyAuthFailed(msg);
            return new ActionResult(false, msg, null);
        }
        if (fullName.isEmpty()) {
            String msg = "กรุณากรอกชื่อ - นามสกุล";
            notifyAuthFailed(msg);
            return new ActionResult(false, msg, null);
        }
        if (faculty.isEmpty()) {
            String msg = "กรุณาเลือกคณะ";
            notifyAuthFailed(msg);
            return new ActionResult(false, msg, null);
        }
        if (instagram.isEmpty()) {
            instagram = "@arpo_cpe";
        }
        if (password.length() < 4) {
            String msg = "รหัสผ่านต้องมีอย่างน้อย 4 ตัวอักษร";
            notifyAuthFailed(msg);
            return new ActionResult(false, msg, null);
        }
        if (userDatabase.containsKey(studentId)) {
            String msg = "รหัสนักศึกษานี้ลงทะเบียนไว้ในระบบแล้ว";
            notifyAuthFailed(msg);
            return new ActionResult(false, msg, null);
        }

        StudentUser newUser = new StudentUser(studentId, fullName, faculty, instagram, line, profileImage, password);
        userDatabase.put(studentId, newUser);
        this.currentUser = newUser;

        // บันทึกลง Persistent Storage จริง
        DataStorage.saveUsers(userDatabase);

        for (AuthEventListener l : listeners) {
            l.onRegisterSuccess(newUser);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentId", newUser.getStudentId());
        data.put("fullName", newUser.getFullName());
        data.put("faculty", newUser.getFaculty());
        data.put("instagram", newUser.getInstagram());
        data.put("phone", newUser.getInstagram());
        data.put("line", newUser.getLine());
        data.put("profileImage", newUser.getProfileImage());
        data.put("role", newUser.getRole());
        data.put("isAdmin", newUser.isAdmin());
        return new ActionResult(true, "สมัครสมาชิกสำเร็จ", data);
    }

    public ActionResult register(String studentId, String fullName, String faculty, String instagram, String profileImage, String password) {
        return register(studentId, fullName, faculty, instagram, "", profileImage, password);
    }

    public ActionResult register(String studentId, String fullName, String faculty, String phone, String password) {
        return register(studentId, fullName, faculty, phone, "", "", password);
    }

    // เมธอดอัปเดตข้อมูลโปรไฟล์ (Instagram และรูปภาพโปรไฟล์)
    public ActionResult updateProfile(String instagram, String profileImage) {
        if (currentUser == null) {
            return new ActionResult(false, "ยังไม่ได้เข้าสู่ระบบ", null);
        }

        if (instagram != null && !instagram.trim().isEmpty()) {
            currentUser.setInstagram(instagram);
        }
        if (profileImage != null && !profileImage.trim().isEmpty()) {
            currentUser.setProfileImage(profileImage);
        }

        // บันทึกลง Persistent Storage จริง
        DataStorage.saveUsers(userDatabase);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentId", currentUser.getStudentId());
        data.put("fullName", currentUser.getFullName());
        data.put("faculty", currentUser.getFaculty());
        data.put("instagram", currentUser.getInstagram());
        data.put("phone", currentUser.getInstagram());
        data.put("line", currentUser.getLine());
        data.put("profileImage", currentUser.getProfileImage());
        data.put("role", currentUser.getRole());
        data.put("isAdmin", currentUser.isAdmin());
        return new ActionResult(true, "อัปเดตข้อมูลโปรไฟล์สำเร็จ", data);
    }

    // --- Admin Operations (การจัดการโดยแอดมิน) ---
    public ActionResult adminDeleteUser(String targetStudentId, StudentUser requester) {
        if (requester == null || !requester.isAdmin()) {
            return new ActionResult(false, "ต้องใช้สิทธิ์แอดมินในการดำเนินการ", null);
        }
        if (targetStudentId == null || targetStudentId.trim().isEmpty()) {
            return new ActionResult(false, "กรุณาระบุรหัสนักศึกษาที่ต้องการลบ", null);
        }
        targetStudentId = targetStudentId.trim();
        if ("6812247050".equals(targetStudentId) || "6812247020".equals(targetStudentId)) {
            return new ActionResult(false, "ไม่สามารถลบบัญชี Super Admin ได้", null);
        }
        if (!userDatabase.containsKey(targetStudentId)) {
            return new ActionResult(false, "ไม่พบบัญชีผู้ใช้รหัส " + targetStudentId, null);
        }
        userDatabase.remove(targetStudentId);
        DataStorage.saveUsers(userDatabase);
        return new ActionResult(true, "ลบบัญชีผู้ใช้รหัส " + targetStudentId + " เรียบร้อยแล้ว", null);
    }

    public ActionResult adminResetPassword(String targetStudentId, String newPassword, StudentUser requester) {
        if (requester == null || !requester.isAdmin()) {
            return new ActionResult(false, "ต้องใช้สิทธิ์แอดมินในการดำเนินการ", null);
        }
        if (targetStudentId == null || targetStudentId.trim().isEmpty()) {
            return new ActionResult(false, "กรุณาระบุรหัสนักศึกษา", null);
        }
        targetStudentId = targetStudentId.trim();
        StudentUser user = userDatabase.get(targetStudentId);
        if (user == null) {
            return new ActionResult(false, "ไม่พบบัญชีผู้ใช้รหัส " + targetStudentId, null);
        }
        String pwd = (newPassword != null && !newPassword.trim().isEmpty()) ? newPassword.trim() : "123456";
        user.setPassword(pwd);
        DataStorage.saveUsers(userDatabase);
        return new ActionResult(true, "รีเซ็ตรหัสผ่านสำหรับรหัส " + targetStudentId + " เป็น " + pwd + " สำเร็จ", null);
    }

    // เมธอด Logout
    public ActionResult logout() {
        this.currentUser = null;
        for (AuthEventListener l : listeners) {
            l.onLogout();
        }
        return new ActionResult(true, "ออกจากระบบแล้ว", null);
    }

    private void notifyAuthFailed(String error) {
        for (AuthEventListener l : listeners) {
            l.onAuthFailed(error);
        }
    }

    public StudentUser getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(StudentUser user) {
        this.currentUser = user;
    }

    public StudentUser getUser(String studentId) {
        return (studentId != null) ? userDatabase.get(studentId.trim()) : null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public int getUserCount() {
        return userDatabase.size();
    }

    public List<StudentUser> getAllUsers() {
        return new ArrayList<>(userDatabase.values());
    }
}
