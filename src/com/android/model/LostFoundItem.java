package com.android.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ============================================================================
 * [Lecture 3: OOP Class and Object]
 * [Lecture 4: OOP Concepts - Encapsulation (การห่อหุ้ม)]
 * ============================================================================
 * 
 * คลาส Model สำหรับแทนข้อมูลของประกาศของหายและแจ้งพบของ (PSRU Lost & Found)
 * 
 * คุณลักษณะสำคัญ:
 * 1. ใช้ Encapsulation ควบคุมการเข้าถึงตัวแปรทั้งหมดผ่าน Getter / Setter
 * 2. รองรับ Method Overloading ในส่วนของ Constructor เพื่อความสะดวกในการสร้างประกาศ
 * 3. มีเมธอด toMap() สำหรับแปลงข้อมูลเป็น Map เพื่อ serialize เป็น JSON
 * 
 * @author CPE Student
 * @version 1.0
 */
public class LostFoundItem {

    // --- Attributes ประจำประกาศ (Encapsulation) ---

    /** รหัสประกาศประจำรายการ เช่น ITEM-101 */
    private final String id;

    /** หัวข้อประกาศ เช่น "กระเป๋าสตางค์สีดำ", "iPad Air 5 สีฟ้า" */
    private String title;

    /** ประเภทของประกาศ: "LOST" (ตามหาของหาย) หรือ "FOUND" (แจ้งพบของ) */
    private String type;

    /** หมวดหมู่สิ่งของ เช่น อุปกรณ์อิเล็กทรอนิกส์, เอกสาร/บัตร, กระเป๋า, กุญแจ */
    private String category;

    /** สถานที่พบหรือจุดที่ทำของหาย เช่น หน้าตึกคณะวิศวะ, โรงอาหารกลาง */
    private String location;

    /** รายละเอียดเพิ่มเติมเกี่ยวกับสิ่งของ */
    private String description;

    /** ที่อยู่รูปภาพสิ่งของ (Local Image URL หรือ Data URL) */
    private String photoUrl;

    /** รหัสนักศึกษาของผู้โพสต์ */
    private final String reporterId;

    /** ชื่อ-นามสกุลของผู้โพสต์ */
    private final String reporterName;

    /** คณะสังกัดของผู้โพสต์ */
    private final String reporterFaculty;

    /** ช่องทางติดต่อ Instagram (IG) ของผู้โพสต์ */
    private String reporterIg;

    /** รูปภาพโปรไฟล์ส่วนตัวของผู้โพสต์ */
    private String reporterPhoto;

    /** เวลาที่สร้างประกาศ (Epoch Timestamp มิลลิวินาที) */
    private final long createdAt;

    /** สถานะของประกาศ: "OPEN" (ยังตามหาอยู่) หรือ "RESOLVED" (ส่งคืน/พบของแล้ว) */
    private String status;

    /**
     * [Lecture 3: Constructor เต็มรูปแบบ]
     * 
     * @param id รหัสประกาศ
     * @param title หัวข้อ
     * @param type ประเภท ("LOST" / "FOUND")
     * @param category หมวดหมู่
     * @param location สถานที่
     * @param description รายละเอียด
     * @param photoUrl รูปภาพสิ่งของ
     * @param reporterId รหัสนักศึกษาผู้โพสต์
     * @param reporterName ชื่อผู้โพสต์
     * @param reporterFaculty คณะผู้โพสต์
     * @param reporterIg บัญชี IG
     * @param reporterPhoto รูปโปรไฟล์ผู้โพสต์
     * @param status สถานะประกาศ ("OPEN" / "RESOLVED")
     */
    public LostFoundItem(String id, String title, String type, String category,
                         String location, String description, String photoUrl,
                         String reporterId, String reporterName, String reporterFaculty,
                         String reporterIg, String reporterPhoto, String status) {
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
        
        // จัดรูปแบบ Instagram ให้ขึ้นต้นด้วย @ เสมอ
        if (reporterIg != null && !reporterIg.trim().isEmpty()) {
            String ig = reporterIg.trim();
            this.reporterIg = ig.startsWith("@") ? ig : "@" + ig;
        } else {
            this.reporterIg = "@arpo_cpe";
        }

        this.reporterPhoto = (reporterPhoto != null) ? reporterPhoto : "";
        this.createdAt = System.currentTimeMillis();
        this.status = (status != null && !status.trim().isEmpty()) ? status : "OPEN";
    }

    /**
     * [Lecture 4: Method Overloading] Constructor แบบไม่ระบุรูปโปรไฟล์
     */
    public LostFoundItem(String id, String title, String type, String category,
                         String location, String description, String photoUrl,
                         String reporterId, String reporterName, String reporterFaculty,
                         String reporterIg, String status) {
        this(id, title, type, category, location, description, photoUrl,
             reporterId, reporterName, reporterFaculty, reporterIg, "", status);
    }

    /**
     * [Lecture 4: Method Overloading] Constructor รับ Object StudentUser โดยตรง
     */
    public LostFoundItem(String id, String title, String type, String category,
                         String location, String description, String photoUrl,
                         StudentUser user) {
        this(id, title, type, category, location, description, photoUrl,
             user != null ? user.getStudentId() : "GUEST",
             user != null ? user.getFullName() : "ผู้ใช้งานนิรนาม",
             user != null ? user.getFaculty() : "คณะวิทยาศาสตร์และเทคโนโลยี",
             user != null ? user.getInstagram() : "@arpo_cpe",
             user != null ? user.getProfileImage() : "",
             "OPEN");
    }

    // ============================================================================
    // Getter & Setter Methods (Encapsulation)
    // ============================================================================

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

    public String getReporterPhoto() {
        return reporterPhoto;
    }

    public void setReporterPhoto(String reporterPhoto) {
        this.reporterPhoto = (reporterPhoto != null) ? reporterPhoto : "";
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

    /**
     * ตรวจสอบว่าประกาศนี้ได้รับการส่งคืน/แก้ไขเรียบร้อยแล้วหรือไม่
     * @return true ถ้าสถานะเป็น RESOLVED
     */
    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(this.status);
    }

    /**
     * เปลี่ยนสถานะประกาศเป็น RESOLVED (ส่งคืนสำเร็จ)
     */
    public void markResolved() {
        this.status = "RESOLVED";
    }

    /**
     * แปลง Object เป็น Map<String, Object> สำหรับ Serialization เป็น JSON ส่งไปหน้า Web
     * @return Map ของข้อมูลประกาศ
     */
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
        map.put("reporterPhoto", reporterPhoto);
        map.put("createdAt", createdAt);
        map.put("status", status);
        return map;
    }
}
