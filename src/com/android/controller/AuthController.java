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
 * ============================================================================
 * [Lecture 5.2: MVC Architectural Pattern - Controller (ส่วนควบคุม)]
 * ============================================================================
 * 
 * คลาส AuthController ทำหน้าที่เป็น Controller ควบคุมตรรกะทางธุรกิจเกี่ยวกับการยืนยันตัวตน
 * 
 * หน้าที่หลัก:
 * 1. ตรวจสอบความถูกต้อง (Validation) ของข้อมูลเข้าสู่ระบบและการสมัครสมาชิก
 * 2. บันทึกและซิงค์ข้อมูลผู้ใช้งานลงสู่ไฟล์ถาวร (data/users.dat) ผ่าน DataStorage
 * 3. แจ้งเตือนเหตุการณ์ (Event Notification) ไปยัง AuthEventListener เมื่อเกิดการ Login/Register/Logout
 * 4. ควบคุมสิทธิ์การจัดการระดับผู้ดูแลระบบ (Admin Operations) เช่น การลบบัญชีและรีเซ็ตรหัสผ่าน
 * 
 * @author CPE Student
 * @version 1.0
 */
public class AuthController {

    /** 
     * ฐานข้อมูลผู้ใช้ในหน่วยความจำ RAM (In-Memory Map)
     * ใช้ Map<studentId, StudentUser> เพื่อการค้นหาที่รวดเร็วระดับ O(1)
     */
    private final Map<String, StudentUser> userDatabase;

    /** อ้างอิงผู้ใช้งานที่กำลังเข้าสู่ระบบอยู่ในปัจจุบัน (Current Session) */
    private StudentUser currentUser;

    /** รายการผู้รับฟังเหตุการณ์ (Event Listeners) ตาม Observer Pattern [Lecture 5.1] */
    private final List<AuthEventListener> listeners;

    /**
     * [Lecture 3: Constructor]
     * กำหนดค่าเริ่มต้นให้กับ Controller โดยโหลดข้อมูลผู้ใช้งานจากไฟล์ถาวร
     */
    public AuthController() {
        // โหลดข้อมูลเฉพาะที่ผู้ใช้งานสมัครสมาชิกไว้จริงจาก data/users.dat
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
                "",
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

    /**
     * ลงทะเบียนผู้รับฟังเหตุการณ์การยืนยันตัวตน [Lecture 5.1: Event-driven]
     * @param listener ผู้รับฟังเหตุการณ์
     */
    public void addListener(AuthEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * [Lecture 2: Logic & Validation]
     * ดำเนินการเข้าสู่ระบบ (Login)
     * 1. ตรวจสอบว่าได้กรอกรหัสนักศึกษาและรหัสผ่านหรือไม่
     * 2. ค้นหาผู้ใช้ใน userDatabase และตรวจสอบความถูกต้องของรหัสผ่าน
     * 3. ปรับค่า currentUser และแจ้งเตือนผ่าน Event Listener
     * 
     * @param studentId รหัสนักศึกษา 10 หลัก
     * @param password รหัสผ่าน
     * @return ActionResult ผลการเข้าสู่ระบบพร้อมข้อมูลผู้ใช้
     */
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

    /**
     * [Lecture 2: Validation] & [Lecture 4: Overloading]
     * ลงทะเบียนสมาชิกใหม่เข้าสู่ระบบ (Register)
     * มีการตรวจสอบความถูกต้องของข้อมูล (Validation):
     * - รหัสนักศึกษาต้องมีความยาว 10 หลัก
     * - ชื่อ-นามสกุล และคณะต้องไม่ว่างเปล่า
     * - รหัสผ่านต้องมีอย่างน้อย 4 ตัวอักษร
     * - รหัสนักศึกษาต้องไม่ซ้ำกับที่มีอยู่แล้วในระบบ
     * 
     * @param studentId รหัสนักศึกษา
     * @param fullName ชื่อ-นามสกุล
     * @param faculty คณะสังกัด
     * @param instagram บัญชี Instagram
     * @param line บัญชี LINE
     * @param profileImage ที่อยู่รูปโปรไฟล์
     * @param password รหัสผ่าน
     * @return ActionResult ผลการลงทะเบียน
     */
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

        // บันทึกลง Persistent Storage จริงทันที
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

    /** Overloaded register() แบบ 6 พารามิเตอร์ */
    public ActionResult register(String studentId, String fullName, String faculty, String instagram, String profileImage, String password) {
        return register(studentId, fullName, faculty, instagram, "", profileImage, password);
    }

    /** Overloaded register() แบบ 5 พารามิเตอร์ */
    public ActionResult register(String studentId, String fullName, String faculty, String phone, String password) {
        return register(studentId, fullName, faculty, phone, "", "", password);
    }

    /**
     * อัปเดตข้อมูลโปรไฟล์ส่วนตัวของผู้ใช้ปัจจุบัน (Instagram และรูปโปรไฟล์)
     * @param instagram บัญชี IG ใหม่
     * @param profileImage รูปโปรไฟล์ใหม่
     * @return ActionResult
     */
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

        // บันทึกการเปลี่ยนแปลงลงฐานข้อมูลไฟล์จริง
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

    // ============================================================================
    // Admin Operations (คำสั่งสำหรับผู้ดูแลระบบเท่านั้น)
    // ============================================================================

    /**
     * สั่งลบบัญชีผู้ใช้งานโดย Admin
     * มีระบบป้องกันไม่ให้ลบบัญชี Super Admin (6812247050 และ 6812247020)
     * 
     * @param targetStudentId รหัสนักศึกษาเป้าหมายที่จะลบ
     * @param requester ผู้ใช้งานที่ส่งคำสั่งลบ (ต้องมีสิทธิ์ Admin)
     * @return ActionResult
     */
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

    /**
     * สั่งรีเซ็ตรหัสผ่านผู้ใช้งานโดย Admin
     * 
     * @param targetStudentId รหัสนักศึกษาเป้าหมาย
     * @param newPassword รหัสผ่านใหม่ที่ต้องการตั้ง
     * @param requester ผู้ใช้งานที่ส่งคำสั่งรีเซ็ต (ต้องมีสิทธิ์ Admin)
     * @return ActionResult
     */
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

    /**
     * สั่งออกจากระบบ (Logout) และเคลียร์ค่า currentUser
     * @return ActionResult
     */
    public ActionResult logout() {
        this.currentUser = null;
        for (AuthEventListener l : listeners) {
            l.onLogout();
        }
        return new ActionResult(true, "ออกจากระบบแล้ว", null);
    }

    /**
     * แจ้งเตือนข้อผิดพลาดในการตรวจสอบสิทธิ์ไปยัง Listener
     * @param error ข้อความแจ้งเตือนความผิดพลาด
     */
    private void notifyAuthFailed(String error) {
        for (AuthEventListener l : listeners) {
            l.onAuthFailed(error);
        }
    }

    // ============================================================================
    // Getter & Setter Methods
    // ============================================================================

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
