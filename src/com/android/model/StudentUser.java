package com.android.model;

/**
 * [Lecture 3: OOP Class and Object]
 * [Lecture 4: OOP Concepts - Encapsulation]
 *
 * คลาส Model ข้อมูลนักศึกษาสำหรับระบบ CPE Portal / PSRU Lost and Found
 * รองรับข้อมูลติดต่อเฉพาะ Instagram (IG) และรูปโปรไฟล์ส่วนตัว
 */
public class StudentUser {
    private final String studentId;   // รหัสนักศึกษา 10 หลัก
    private final String fullName;    // ชื่อ - นามสกุล
    private final String faculty;     // คณะสังกัด (1 ใน 8 คณะ)
    private String instagram;         // Contact: Instagram (IG)
    private String line;              // Contact: Line ID
    private String profileImage;      // รูปภาพโปรไฟล์ส่วนตัว (Data URL หรือ Image Path)
    private String password;          // รหัสผ่าน (แก้ไขได้กรณีแอดมินรีเซ็ต)
    private String role;              // สิทธิ์ผู้ใช้งาน: "ADMIN" หรือ "STUDENT"
    private final long registeredAt;  // เวลาที่ลงทะเบียน

    // [Lecture 3: Constructor] Constructor เต็มรูปแบบ (8 พารามิเตอร์)
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String line, String profileImage, String password, String role) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.faculty = (faculty != null && !faculty.trim().isEmpty()) ? faculty : "คณะวิศวกรรมศาสตร์และเทคโนโลยีอุตสาหกรรม";
        
        // จัดรูปแบบชื่อ Instagram ให้ขึ้นต้นด้วย @ เสมอ
        if (instagram != null && !instagram.trim().isEmpty()) {
            String ig = instagram.trim();
            this.instagram = ig.startsWith("@") ? ig : "@" + ig;
        } else {
            this.instagram = "@arpo_cpe";
        }

        this.line = (line != null) ? line.trim() : "";
        this.profileImage = (profileImage != null) ? profileImage.trim() : "";
        this.password = password;
        
        // รหัส 6812247050 และ 6812247020 ได้รับสิทธิ์ ADMIN อัตโนมัติเสมอ
        if ("6812247050".equals(studentId) || "6812247020".equals(studentId)) {
            this.role = "ADMIN";
        } else {
            this.role = (role != null && !role.trim().isEmpty()) ? role.trim().toUpperCase() : "STUDENT";
        }
        
        this.registeredAt = System.currentTimeMillis();
    }

    // [Lecture 4: Method Overloading] Constructor พร้อม line (7 parameters)
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String line, String profileImage, String password) {
        this(studentId, fullName, faculty, instagram, line, profileImage, password, ("6812247050".equals(studentId) || "6812247020".equals(studentId)) ? "ADMIN" : "STUDENT");
    }

    // [Lecture 4: Method Overloading] Constructor พื้นฐาน (6 parameters)
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String profileImage, String password) {
        this(studentId, fullName, faculty, instagram, "", profileImage, password);
    }

    // [Lecture 4: Method Overloading] Constructor เสริมสำหรับความเข้ากันได้ (5 parameters)
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String password) {
        this(studentId, fullName, faculty, instagram, "", "", password);
    }

    // --- Getter & Setter Methods (Encapsulation) ---
    public String getStudentId() {
        return studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getFaculty() {
        return faculty;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        if (instagram != null && !instagram.trim().isEmpty()) {
            String ig = instagram.trim();
            this.instagram = ig.startsWith("@") ? ig : "@" + ig;
        }
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = (profileImage != null) ? profileImage.trim() : "";
    }

    public String getLine() {
        return line != null ? line : "";
    }

    public void setLine(String line) {
        this.line = (line != null) ? line.trim() : "";
    }

    // เมธอด getPhone สำหรับความเข้ากันได้ย้อนหลัง
    public String getPhone() {
        return instagram;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean checkPassword(String pwd) {
        return this.password != null && this.password.equals(pwd);
    }

    // --- Role-Based Access Control (RBAC) ---
    public String getRole() {
        if ("6812247050".equals(studentId) || "6812247020".equals(studentId)) {
            return "ADMIN";
        }
        return (role != null && !role.trim().isEmpty()) ? role.trim().toUpperCase() : "STUDENT";
    }

    public void setRole(String role) {
        if ("6812247050".equals(studentId) || "6812247020".equals(studentId)) {
            this.role = "ADMIN"; // รหัส 6812247050 และ 6812247020 เป็น ADMIN ถาวร
        } else {
            this.role = (role != null) ? role.trim().toUpperCase() : "STUDENT";
        }
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getRole()) || "6812247050".equals(studentId) || "6812247020".equals(studentId);
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("studentId", getStudentId());
        map.put("fullName", getFullName());
        map.put("faculty", getFaculty());
        map.put("instagram", getInstagram());
        map.put("phone", getInstagram());
        map.put("line", getLine());
        map.put("profileImage", getProfileImage());
        map.put("role", getRole());
        map.put("isAdmin", isAdmin());
        map.put("registeredAt", getRegisteredAt());
        return map;
    }
}
