package com.android.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [Lecture 3: OOP Class and Object]
 * [Lecture 4: OOP Concepts - Encapsulation]
 *
 * คลาส Model สำหรับโพสต์ประกาศของหายและแจ้งพบของ (PSRU Lost & Found)
 * ใช้หลักการ Encapsulation โดยตัวแปรทั้งหมดเป็น private และเข้าถึงผ่าน Getter/Setter
 */
public class LostFoundItem {
    private final String id;             // รหัสประกาศ เช่น ITEM-101
    private String title;                 // หัวข้อประกาศ เช่น กระเป๋าสตางค์สีดำ, iPad Air
    private String type;                  // ประเภท: "LOST" (ตามหาของหาย) หรือ "FOUND" (แจ้งพบของหาย)
    private String category;              // หมวดหมู่ เช่น อุปกรณ์อิเล็กทรอนิกส์, เอกสาร/บัตร, กระเป๋า, กุญแจ
    private String location;              // สถานที่พบหรือจุดที่ทำหาย
    private String description;           // รายละเอียดเพิ่มเติม
    private String photoUrl;              // รูปภาพสิ่งของ (Data URL หรือ URL รูปภาพ)
    private final String reporterId;      // รหัสนักศึกษาของผู้แจ้ง
    private final String reporterName;    // ชื่อ-นามสกุลของผู้แจ้ง
    private final String reporterFaculty; // คณะสังกัดของผู้แจ้ง
    private String reporterIg;            // ช่องทางติดต่อ: Instagram (IG)
    private final long createdAt;         // เวลาที่โพสต์ (timestamp)
    private String status;                // สถานะ: "OPEN" (ยังตามหาอยู่) หรือ "RESOLVED" (ส่งคืนแล้ว)

    // [Lecture 3: Constructor]
    public LostFoundItem(String id, String title, String type, String category,
                         String location, String description, String photoUrl,
                         String reporterId, String reporterName, String reporterFaculty,
                         String reporterIg, String status) {
        this.id = id;
        this.title = title;
        this.type = (type != null && type.equalsIgnoreCase("FOUND")) ? "FOUND" : "LOST";
        this.category = (category != null && !category.trim().isEmpty()) ? category : "ทั่วไป";
        this.location = location;
        this.description = description;
        this.photoUrl = (photoUrl != null) ? photoUrl : "";
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.reporterFaculty = reporterFaculty;
        
        // จัดรูปแบบ Instagram ให้มี @ เสมอ
        if (reporterIg != null && !reporterIg.trim().isEmpty()) {
            String ig = reporterIg.trim();
            this.reporterIg = ig.startsWith("@") ? ig : "@" + ig;
        } else {
            this.reporterIg = "@arpo_cpe";
        }

        this.createdAt = System.currentTimeMillis();
        this.status = (status != null && !status.trim().isEmpty()) ? status : "OPEN";
    }

    // [Lecture 4: Method Overloading]
    public LostFoundItem(String id, String title, String type, String category,
                         String location, String description, String photoUrl,
                         StudentUser user) {
        this(id, title, type, category, location, description, photoUrl,
             user != null ? user.getStudentId() : "GUEST",
             user != null ? user.getFullName() : "ผู้ใช้งานนิรนาม",
             user != null ? user.getFaculty() : "คณะวิทยาศาสตร์และเทคโนโลยี",
             user != null ? user.getInstagram() : "@arpo_cpe",
             "OPEN");
    }

    // --- Getter & Setter Methods (Encapsulation) ---
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getReporterId() {
        return reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getReporterFaculty() {
        return reporterFaculty;
    }

    public String getReporterIg() {
        return reporterIg;
    }

    public void setReporterIg(String reporterIg) {
        this.reporterIg = reporterIg;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(this.status);
    }

    public void markResolved() {
        this.status = "RESOLVED";
    }

    // แปลงข้อมูลเป็น Map สำหรับการแปลงเป็น JSON [Lecture 5.2 MVC View]
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("type", type);
        map.put("category", category);
        map.put("location", location);
        map.put("description", description);
        map.put("photoUrl", photoUrl);
        map.put("reporterId", reporterId);
        map.put("reporterName", reporterName);
        map.put("reporterFaculty", reporterFaculty);
        map.put("reporterIg", reporterIg);
        map.put("createdAt", createdAt);
        map.put("status", status);
        return map;
    }
}
