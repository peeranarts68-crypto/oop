package com.android.model.apps;

import com.android.controller.AuthController;
import com.android.model.AndroidPhone;
import com.android.model.DataStorage;
import com.android.model.LostFoundItem;
import com.android.model.StudentUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [Lecture 4: Inheritance & Polymorphism]
 *
 * แอปพลิเคชัน PSRU Lost and Found / CPE Portal
 * จัดการข้อมูลฟีดประกาศตามหาของหายและแจ้งพบของ โดยเก็บเฉพาะข้อมูลจริงที่ผู้ใช้งานโพสต์เข้ามาเท่านั้น
 * ไม่มีการสร้างข้อมูลสุ่มหรือ mock seed data ขึ้นมาเอง
 */
public class PsruLostFoundApp extends AndroidApplication {
    private final AndroidPhone phone;
    private final AuthController authController;
    private final List<LostFoundItem> itemList;
    private final AtomicInteger itemCounter;

    public PsruLostFoundApp(AndroidPhone phone) {
        this(phone, new AuthController());
    }

    public PsruLostFoundApp(AndroidPhone phone, AuthController authController) {
        super("com.psru.lostfound", "PSRU Lost and Found", "psru", "1.0.0", 38);
        this.phone = phone;
        this.authController = authController;
        
        // โหลดเฉพาะประกาศจริงที่ผู้ใช้งานบันทึกไว้ (ไม่มีการสร้างข้อมูลมั่ว)
        this.itemList = new CopyOnWriteArrayList<>(DataStorage.loadPosts());
        this.itemCounter = new AtomicInteger(itemList.size() + 100);
    }

    @Override
    public ActionResult executeAction(String action, Map<String, Object> params) {
        if ("LOGIN".equalsIgnoreCase(action)) {
            String studentId = params != null && params.containsKey("studentId") ? String.valueOf(params.get("studentId")) : "";
            String password = params != null && params.containsKey("password") ? String.valueOf(params.get("password")) : "";
            ActionResult result = authController.login(studentId, password);
            if (result.isSuccess() && authController.getCurrentUser() != null) {
                phone.getOs().getNotificationManager().postNotification(
                    "PSRU Lost & Found",
                    "ยินดีต้อนรับเข้าสู่ระบบ " + authController.getCurrentUser().getFullName(),
                    "PSRU App"
                );
            }
            return result;

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

        } else if ("UPDATE_PROFILE".equalsIgnoreCase(action)) {
            String instagram = params != null && params.containsKey("instagram") ? String.valueOf(params.get("instagram")) : "";
            String profileImage = params != null && params.containsKey("profileImage") ? String.valueOf(params.get("profileImage")) : "";
            return authController.updateProfile(instagram, profileImage);

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
                "OPEN"
            );

            // โพสต์ใหม่ให้อยู่ด้านบนสุด (index 0)
            itemList.add(0, newItem);

            // บันทึกลง Persistent Storage จริง
            DataStorage.savePosts(itemList);

            // [Lecture 5.1: Event-driven] ส่ง Notification เข้าสู่ NotificationManager ของมือถือ
            String typeLabel = "LOST".equalsIgnoreCase(type) ? "ตามหาของหาย: " : "แจ้งพบของ: ";
            phone.getOs().getNotificationManager().postNotification(
                "โพสต์ประกาศใหม่",
                typeLabel + title + " (" + location + ")",
                "PSRU Lost & Found"
            );

            return new ActionResult(true, "โพสต์ประกาศสำเร็จเรียบร้อยแล้ว", newItem.toMap());

        } else if ("DELETE_POST".equalsIgnoreCase(action) || "DELETE_ITEM".equalsIgnoreCase(action)) {
            String itemId = params != null && params.containsKey("itemId") ? String.valueOf(params.get("itemId")) : "";
            if (itemId.isEmpty()) {
                return new ActionResult(false, "ไม่พบรหัสประกาศ", null);
            }

            StudentUser curUser = authController.getCurrentUser();
            boolean removed = itemList.removeIf(item -> {
                if (item.getId().equalsIgnoreCase(itemId)) {
                    // อนุญาตถ้าเป็นเจ้าของโพสต์ หรือเป็น Admin
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

        } else if ("ADMIN_DELETE_USER".equalsIgnoreCase(action)) {
            String targetId = params != null && params.containsKey("studentId") ? String.valueOf(params.get("studentId")) : "";
            return authController.adminDeleteUser(targetId, authController.getCurrentUser());

        } else if ("ADMIN_RESET_PASSWORD".equalsIgnoreCase(action)) {
            String targetId = params != null && params.containsKey("studentId") ? String.valueOf(params.get("studentId")) : "";
            String newPwd = params != null && params.containsKey("newPassword") ? String.valueOf(params.get("newPassword")) : "123456";
            return authController.adminResetPassword(targetId, newPwd, authController.getCurrentUser());

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
            return new ActionResult(true, "ส่งประกาศด่วนขึ้นระบบสำเร็จ", null);

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

        } else if ("LOGOUT".equalsIgnoreCase(action)) {
            return authController.logout();
        }

        return new ActionResult(false, "Unknown action: " + action, null);
    }

    @Override
    public Object getAppData() {
        Map<String, Object> data = new HashMap<>();
        boolean loggedIn = authController.isLoggedIn();
        data.put("isLoggedIn", loggedIn);

        StudentUser user = authController.getCurrentUser();
        if (user != null) {
            Map<String, Object> u = user.toMap();
            data.put("currentUser", u);

            // หากเป็นแอดมิน ให้ส่งข้อมูลรายชื่อผู้ใช้งานทั้งหมดเพื่อจัดการใน Admin Panel
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

        // ข้อมูลรายการประกาศของหายและแจ้งพบของที่สร้างโดย user เท่านั้น
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

    public AuthController getAuthController() {
        return authController;
    }

    public List<LostFoundItem> getItemList() {
        return Collections.unmodifiableList(itemList);
    }
}
