package com.android.model.apps;

import com.android.controller.AuthController;
import com.android.model.AndroidPhone;
import com.android.model.DataStorage;
import com.android.model.LostFoundItem;
import com.android.model.StudentUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================================
 * [Lecture 4: OOP Concepts - Inheritance & Polymorphism]
 * [Lecture 5.2: MVC Pattern - Application Business Logic]
 * ============================================================================
 * 
 * แอปพลิเคชัน PSRU Lost and Found / CPE Student Portal
 * - สืบทอด (Inheritance) มาจากคลาสแม่ AndroidApplication
 * - จัดการข้อมูลฟีดประกาศตามหาของหายและแจ้งพบของ โดยเก็บข้อมูลจริงที่ผู้ใช้งานบันทึกไว้ใน DataStorage
 * - รองรับระบบ Login, Register, Profile, Post CRUD, และ Admin Panel
 * - นำเสนอ Polymorphism ผ่านการ Override เมธอด executeAction() เพื่อจัดการคำสั่งต่างๆ
 * 
 * @author CPE Student
 * @version 1.0
 */
public class PsruLostFoundApp extends AndroidApplication {

    /** อ้างอิงสมาร์ตโฟนที่เป็นโฮสต์ของแอปพลิเคชัน */
    private final AndroidPhone phone;

    /** คอนโทรลเลอร์ควบคุมการตรวจสอบสิทธิ์และบัญชีผู้ใช้งาน */
    private final AuthController authController;

    /** 
     * รายการจัดเก็บประกาศของหายและแจ้งพบของ
     * ใช้ CopyOnWriteArrayList เพื่อความปลอดภัยในการเข้าถึงพร้อมกันแบบ Multithread (Thread-Safe)
     */
    private final List<LostFoundItem> itemList;

    /** ตัวนับรหัสประกาศแบบ Thread-Safe สำหรับสร้าง ID ใหม่ เช่น ITEM-101, ITEM-102 */
    private final AtomicInteger itemCounter;

    /**
     * [Lecture 3: Constructor]
     * ตัวสร้างแอปพลิเคชันโดยใช้ AuthController ตัวใหม่
     * @param phone สมาร์ตโฟนเครื่องที่ติดตั้งแอป
     */
    public PsruLostFoundApp(AndroidPhone phone) {
        this(phone, new AuthController());
    }

    /**
     * [Lecture 4: Method Overloading]
     * ตัวสร้างแอปพลิเคชันโดยระบุ AuthController ร่วม
     * @param phone สมาร์ตโฟนเครื่องที่ติดตั้งแอป
     * @param authController คอนโทรลเลอร์ยืนยันตัวตน
     */
    public PsruLostFoundApp(AndroidPhone phone, AuthController authController) {
        super("com.psru.lostfound", "PSRU Lost and Found", "psru", "1.0.0", 38);
        this.phone = phone;
        this.authController = authController;
        
        // โหลดข้อมูลประกาศจริงจาก Persistent Storage (data/posts.dat)
        this.itemList = new CopyOnWriteArrayList<>(DataStorage.loadPosts());
        this.itemCounter = new AtomicInteger(itemList.size() + 100);
    }

    /**
     * [Lecture 4: Polymorphism - Dynamic Method Dispatch]
     * จัดการคำสั่งการกระทำต่างๆ ที่ส่งมาจากผู้ใช้ผ่านหน้าจอ UI
     * 
     * รายการ Action ที่รองรับ:
     * 1. LOGIN : ตรวจสอบรหัสนักศึกษาและรหัสผ่าน
     * 2. RESTORE_SESSION : คืนค่าเซสชันการเข้าสู่ระบบ
     * 3. REGISTER : ลงทะเบียนสมาชิกใหม่
     * 4. UPDATE_PROFILE : อัปเดตข้อมูล IG และรูปภาพโปรไฟล์
     * 5. CREATE_POST : สร้างประกาศของหายหรือแจ้งพบของใหม่
     * 6. DELETE_POST : ลบประกาศ (สำหรับเจ้าของโพสต์หรือ Admin)
     * 7. RESOLVE_POST : ปรับสถานะเป็นส่งคืนแล้ว (Resolved)
     * 8. ADMIN_DELETE_USER : ลบบัญชีผู้ใช้งานโดย Admin
     * 9. ADMIN_RESET_PASSWORD : รีเซ็ตรหัสผ่านผู้ใช้งานโดย Admin
     * 10. ADMIN_BROADCAST : ส่งประกาศด่วนแจ้งเตือนแบบพุช
     * 11. ADMIN_PIN_POST : ปักหมุดประกาศสำคัญขึ้นบนสุด
     * 12. ADMIN_CLEAR_RESOLVED : ล้างประกาศที่ส่งคืนเรียบร้อยแล้ว
     * 13. LOGOUT : ออกจากระบบ
     * 
     * @param action ชื่อคำสั่งที่ต้องการให้แอปทำงาน
     * @param params แผนที่พารามิเตอร์ของคำสั่ง (Map<String, Object>)
     * @return ผลลัพธ์ ActionResult ที่ประกอบด้วย success, message, และ data
     */
    @Override
    public ActionResult executeAction(String action, Map<String, Object> params) {

        // -------------------------------------------------------------
        // 1. คำสั่งเข้าสู่ระบบ (LOGIN)
        // -------------------------------------------------------------
        if ("LOGIN".equalsIgnoreCase(action)) {
            String studentId = params != null && params.containsKey("studentId") ? String.valueOf(params.get("studentId")) : "";
            String password = params != null && params.containsKey("password") ? String.valueOf(params.get("password")) : "";
            ActionResult result = authController.login(studentId, password);
            if (result.isSuccess() && authController.getCurrentUser() != null) {
                // ส่ง Notification แจ้งเตือนเข้าแถบ Notification Bar ของ Android
                phone.getOs().getNotificationManager().postNotification(
                    "PSRU Lost & Found",
                    "ยินดีต้อนรับเข้าสู่ระบบ " + authController.getCurrentUser().getFullName(),
                    "PSRU App"
                );
            }
            return result;

        // -------------------------------------------------------------
        // 2. คำสั่งคืนค่าเซสชันผู้ใช้ (RESTORE_SESSION)
        // -------------------------------------------------------------
        } else if ("RESTORE_SESSION".equalsIgnoreCase(action)) {
            String studentId = params != null && params.containsKey("studentId") ? String.valueOf(params.get("studentId")) : "";
            StudentUser user = authController.getUser(studentId);
            if (user != null) {
                authController.setCurrentUser(user);
                return new ActionResult(true, "คืนค่าเซสชันสำเร็จ", user.toMap());
            }
            return new ActionResult(false, "ไม่พบบัญชีผู้ใช้", null);

        // -------------------------------------------------------------
        // 3. คำสั่งลงทะเบียนสมาชิกใหม่ (REGISTER)
        // -------------------------------------------------------------
        } else if ("REGISTER".equalsIgnoreCase(action)) {
            String studentId = params != null && params.containsKey("studentId") ? String.valueOf(params.get("studentId")) : "";
            String fullName = params != null && params.containsKey("fullName") ? String.valueOf(params.get("fullName")) : "";
            String faculty = params != null && params.containsKey("faculty") ? String.valueOf(params.get("faculty")) : "";
            
            String instagram = "";
            if (params != null) {
                if (params.containsKey("instagram")) {
                    instagram = String.valueOf(params.get("instagram"));
                } else if (params.containsKey("phone")) {
                    instagram = String.valueOf(params.get("phone"));
                }
            }
            String profileImage = params != null && params.containsKey("profileImage") ? String.valueOf(params.get("profileImage")) : "";
            String password = params != null && params.containsKey("password") ? String.valueOf(params.get("password")) : "";

            ActionResult result = authController.register(studentId, fullName, faculty, instagram, profileImage, password);
            if (result.isSuccess()) {
                phone.getOs().getNotificationManager().postNotification(
                    "PSRU Lost & Found",
                    "ลงทะเบียนสมาชิกสำเร็จ: " + fullName,
                    "PSRU App"
                );
            }
            return result;

        // -------------------------------------------------------------
        // 4. คำสั่งอัปเดตข้อมูลโปรไฟล์ (UPDATE_PROFILE)
        // -------------------------------------------------------------
        } else if ("UPDATE_PROFILE".equalsIgnoreCase(action)) {
            String instagram = params != null && params.containsKey("instagram") ? String.valueOf(params.get("instagram")) : "";
            String profileImage = params != null && params.containsKey("profileImage") ? String.valueOf(params.get("profileImage")) : "";
            return authController.updateProfile(instagram, profileImage);

        // -------------------------------------------------------------
        // 5. คำสั่งสร้างประกาศใหม่ (CREATE_POST / ADD_ITEM)
        // -------------------------------------------------------------
        } else if ("CREATE_POST".equalsIgnoreCase(action) || "ADD_ITEM".equalsIgnoreCase(action)) {
            // ต้องเข้าสู่ระบบก่อนจึงจะโพสต์ได้
            if (!authController.isLoggedIn() || authController.getCurrentUser() == null) {
                return new ActionResult(false, "กรุณาเข้าสู่ระบบก่อนโพสต์ประกาศ", null);
            }

            StudentUser curUser = authController.getCurrentUser();
            String title = params != null && params.containsKey("title") ? String.valueOf(params.get("title")).trim() : "";
            String type = params != null && params.containsKey("type") ? String.valueOf(params.get("type")).trim() : "LOST";
            String category = params != null && params.containsKey("category") ? String.valueOf(params.get("category")).trim() : "ทั่วไป";
            String location = params != null && params.containsKey("location") ? String.valueOf(params.get("location")).trim() : "";
            String description = params != null && params.containsKey("description") ? String.valueOf(params.get("description")).trim() : "";
            String photoUrl = params != null && params.containsKey("photoUrl") ? String.valueOf(params.get("photoUrl")) : "";

            if (title.isEmpty()) {
                return new ActionResult(false, "กรุณาระบุชื่อสิ่งของที่หายหรือพบ", null);
            }
            if (location.isEmpty()) {
                return new ActionResult(false, "กรุณาระบุสถานที่", null);
            }

            String itemId = "ITEM-" + itemCounter.incrementAndGet();
            LostFoundItem newItem = new LostFoundItem(
                itemId,
                title,
                type,
                category,
                location,
                description,
                photoUrl,
                curUser.getStudentId(),
                curUser.getFullName(),
                curUser.getFaculty(),
                curUser.getInstagram(),
                curUser.getProfileImage(),
                "OPEN"
            );

            // วางประกาศใหม่ไว้บนสุดของฟีด (Index 0)
            itemList.add(0, newItem);

            // บันทึกการเปลี่ยนแปลงลงฐานข้อมูลไฟล์จริง (data/posts.dat)
            DataStorage.savePosts(itemList);

            // [Lecture 5.1: Event-driven] ส่ง Notification เข้าแถบแจ้งเตือนของสมาร์ตโฟน
            String typeLabel = "LOST".equalsIgnoreCase(type) ? "ตามหาของหาย: " : "แจ้งพบของ: ";
            phone.getOs().getNotificationManager().postNotification(
                "โพสต์ประกาศใหม่",
                typeLabel + title + " (" + location + ")",
                "PSRU Lost & Found"
            );

            return new ActionResult(true, "โพสต์ประกาศสำเร็จเรียบร้อยแล้ว", newItem.toMap());

        // -------------------------------------------------------------
        // 6. คำสั่งลบประกาศ (DELETE_POST)
        // -------------------------------------------------------------
        } else if ("DELETE_POST".equalsIgnoreCase(action) || "DELETE_ITEM".equalsIgnoreCase(action)) {
            String itemId = params != null && params.containsKey("itemId") ? String.valueOf(params.get("itemId")) : "";
            if (itemId.isEmpty()) {
                return new ActionResult(false, "ไม่พบรหัสประกาศ", null);
            }

            StudentUser curUser = authController.getCurrentUser();
            boolean removed = itemList.removeIf(item -> {
                if (item.getId().equalsIgnoreCase(itemId)) {
                    // อนุญาตให้ลบได้เฉพาะเจ้าของโพสต์ หรือผู้ใช้งานที่มีสิทธิ์ Admin
                    return curUser != null && (curUser.isAdmin() || curUser.getStudentId().equals(item.getReporterId()));
                }
                return false;
            });
            if (removed) {
                DataStorage.savePosts(itemList);
                return new ActionResult(true, "ลบประกาศเรียบร้อยแล้ว", null);
            } else {
                return new ActionResult(false, "ไม่พบประกาศหรือไม่มีสิทธิ์ลบประกาศนี้", null);
            }

        // -------------------------------------------------------------
        // 7. คำสั่งเปลี่ยนสถานะเป็นส่งคืนแล้ว (RESOLVE_POST)
        // -------------------------------------------------------------
        } else if ("RESOLVE_POST".equalsIgnoreCase(action)) {
            String itemId = params != null && params.containsKey("itemId") ? String.valueOf(params.get("itemId")) : "";
            StudentUser curUser = authController.getCurrentUser();
            for (LostFoundItem item : itemList) {
                if (item.getId().equalsIgnoreCase(itemId)) {
                    if (curUser != null && (curUser.isAdmin() || curUser.getStudentId().equals(item.getReporterId()))) {
                        item.markResolved();
                        DataStorage.savePosts(itemList);
                        return new ActionResult(true, "ปรับสถานะเรียบร้อยแล้ว", item.toMap());
                    } else {
                        return new ActionResult(false, "ไม่มีสิทธิ์ปรับสถานะประกาศนี้", null);
                    }
                }
            }
            return new ActionResult(false, "ไม่พบประกาศ", null);

        // -------------------------------------------------------------
        // 8. การจัดการโดย Admin: ลบบัญชีผู้ใช้ (ADMIN_DELETE_USER)
        // -------------------------------------------------------------
        } else if ("ADMIN_DELETE_USER".equalsIgnoreCase(action)) {
            String targetId = params != null && params.containsKey("studentId") ? String.valueOf(params.get("studentId")) : "";
            return authController.adminDeleteUser(targetId, authController.getCurrentUser());

        // -------------------------------------------------------------
        // 9. การจัดการโดย Admin: รีเซ็ตรหัสผ่านผู้ใช้ (ADMIN_RESET_PASSWORD)
        // -------------------------------------------------------------
        } else if ("ADMIN_RESET_PASSWORD".equalsIgnoreCase(action)) {
            String targetId = params != null && params.containsKey("studentId") ? String.valueOf(params.get("studentId")) : "";
            String newPwd = params != null && params.containsKey("newPassword") ? String.valueOf(params.get("newPassword")) : "123456";
            return authController.adminResetPassword(targetId, newPwd, authController.getCurrentUser());

        // -------------------------------------------------------------
        // 10. การจัดการโดย Admin: ส่งประกาศด่วนทั้งระบบ (ADMIN_BROADCAST)
        // -------------------------------------------------------------
        } else if ("ADMIN_BROADCAST".equalsIgnoreCase(action)) {
            StudentUser curUser = authController.getCurrentUser();
            if (curUser == null || !curUser.isAdmin()) {
                return new ActionResult(false, "ต้องใช้สิทธิ์แอดมินในการส่งประกาศด่วน", null);
            }
            String title = params != null && params.containsKey("title") ? String.valueOf(params.get("title")) : "ประกาศจากแอดมิน";
            String message = params != null && params.containsKey("message") ? String.valueOf(params.get("message")) : "";
            if (message.trim().isEmpty()) {
                return new ActionResult(false, "กรุณากรอกข้อความประกาศ", null);
            }
            phone.getOs().getNotificationManager().postNotification(
                "📢 " + title,
                message + " (" + curUser.getFullName() + ")",
                "Admin Broadcast"
            );
            Map<String, Object> bcData = new LinkedHashMap<>();
            bcData.put("id", "BC-" + System.currentTimeMillis());
            bcData.put("title", title);
            bcData.put("message", message);
            bcData.put("senderName", curUser.getFullName());
            bcData.put("senderId", curUser.getStudentId());
            bcData.put("createdAt", System.currentTimeMillis());
            return new ActionResult(true, "ส่งประกาศด่วนขึ้นระบบสำเร็จ", bcData);

        // -------------------------------------------------------------
        // 11. การจัดการโดย Admin: ลบประกาศด่วน (ADMIN_DELETE_BROADCAST)
        // -------------------------------------------------------------
        } else if ("ADMIN_DELETE_BROADCAST".equalsIgnoreCase(action)) {
            StudentUser curUser = authController.getCurrentUser();
            if (curUser == null || !curUser.isAdmin()) {
                return new ActionResult(false, "ต้องใช้สิทธิ์แอดมิน", null);
            }
            return new ActionResult(true, "ลบประกาศด่วนเรียบร้อยแล้ว", null);

        // -------------------------------------------------------------
        // 12. การจัดการโดย Admin: ปักหมุดประกาศขึ้นบนสุด (ADMIN_PIN_POST)
        // -------------------------------------------------------------
        } else if ("ADMIN_PIN_POST".equalsIgnoreCase(action)) {
            StudentUser curUser = authController.getCurrentUser();
            if (curUser == null || !curUser.isAdmin()) {
                return new ActionResult(false, "ต้องใช้สิทธิ์แอดมิน", null);
            }
            String itemId = params != null && params.containsKey("itemId") ? String.valueOf(params.get("itemId")) : "";
            LostFoundItem target = null;
            for (LostFoundItem it : itemList) {
                if (it.getId().equalsIgnoreCase(itemId)) {
                    target = it;
                    break;
                }
            }
            if (target != null) {
                itemList.remove(target);
                itemList.add(0, target);
                DataStorage.savePosts(itemList);
                return new ActionResult(true, "ปักหมุดประกาศขึ้นบนสุดเรียบร้อยแล้ว", target.toMap());
            }
            return new ActionResult(false, "ไม่พบประกาศ", null);

        // -------------------------------------------------------------
        // 13. การจัดการโดย Admin: ล้างประกาศที่ส่งคืนแล้วทั้งหมด (ADMIN_CLEAR_RESOLVED)
        // -------------------------------------------------------------
        } else if ("ADMIN_CLEAR_RESOLVED".equalsIgnoreCase(action)) {
            StudentUser curUser = authController.getCurrentUser();
            if (curUser == null || !curUser.isAdmin()) {
                return new ActionResult(false, "ต้องใช้สิทธิ์แอดมิน", null);
            }
            int before = itemList.size();
            itemList.removeIf(it -> "RESOLVED".equalsIgnoreCase(it.getStatus()));
            int cleared = before - itemList.size();
            DataStorage.savePosts(itemList);
            return new ActionResult(true, "ล้างประกาศที่ส่งคืนแล้วจำนวน " + cleared + " รายการ", null);

        // -------------------------------------------------------------
        // 14. คำสั่งออกจากระบบ (LOGOUT)
        // -------------------------------------------------------------
        } else if ("LOGOUT".equalsIgnoreCase(action)) {
            return authController.logout();
        }

        return new ActionResult(false, "Unknown action: " + action, null);
    }

    /**
     * คืนค่าข้อมูลสถานะของแอปพลิเคชันเพื่อส่งต่อไปแสดงผลบน Web View
     * รวมถึงรายชื่อผู้ใช้ทั้งหมด (กรณีแอดมิน), รายการโพสต์ทั้งหมด, และสถิติต่างๆ
     * 
     * @return Map โครงสร้างข้อมูลแอปพลิเคชัน
     */
    @Override
    public Object getAppData() {
        Map<String, Object> data = new HashMap<>();
        boolean loggedIn = authController.isLoggedIn();
        data.put("isLoggedIn", loggedIn);

        StudentUser user = authController.getCurrentUser();
        if (user != null) {
            Map<String, Object> u = user.toMap();
            data.put("currentUser", u);

            // หากเป็นแอดมิน ให้ส่งข้อมูลรายชื่อผู้ใช้งานทั้งหมดเพื่อนำไปจัดการใน Admin Panel
            if (user.isAdmin()) {
                List<Map<String, Object>> allUsersList = new ArrayList<>();
                for (StudentUser su : authController.getAllUsers()) {
                    allUsersList.add(su.toMap());
                }
                data.put("allUsers", allUsersList);
            }
        } else {
            data.put("currentUser", null);
        }
        data.put("totalUsers", authController.getUserCount());

        // จัดเตรียมข้อมูลรายการประกาศและสถิติ
        List<Map<String, Object>> items = new ArrayList<>();
        int lostCount = 0;
        int foundCount = 0;
        int resolvedCount = 0;
        for (LostFoundItem item : itemList) {
            items.add(item.toMap());
            if ("RESOLVED".equalsIgnoreCase(item.getStatus())) {
                resolvedCount++;
            } else if ("LOST".equalsIgnoreCase(item.getType())) {
                lostCount++;
            } else {
                foundCount++;
            }
        }
        data.put("items", items);
        data.put("totalItems", items.size());
        data.put("lostCount", lostCount);
        data.put("foundCount", foundCount);
        data.put("resolvedCount", resolvedCount);

        return data;
    }

    /**
     * อ่านคอนโทรลเลอร์ตรวจสอบสิทธิ์
     * @return AuthController
     */
    public AuthController getAuthController() {
        return authController;
    }

    /**
     * อ่านรายการประกาศทั้งหมด (แบบ Read-Only Unmodifiable List)
     * @return List ของ LostFoundItem
     */
    public List<LostFoundItem> getItemList() {
        return Collections.unmodifiableList(itemList);
    }
}
