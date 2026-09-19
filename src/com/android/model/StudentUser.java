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
    private String profileImage;      // รูปภาพโปรไฟล์ส่วนตัว (Data URL หรือ Image Path)
    private final String password;    // รหัสผ่าน
    private final long registeredAt;  // เวลาที่ลงทะเบียน

    // [Lecture 3: Constructor]
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String profileImage, String password) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.faculty = (faculty != null && !faculty.trim().isEmpty()) ? faculty : "\u0e04\u0e13\u0e30\u0e27\u0e34\u0e17\u0e22\u0e32\u0e28\u0e32\u0e2a\u0e15\u0e23\u0e4c\u0e41\u0e25\u0e30\u0e40\u0e17\u0e04\u0e42\u0e19\u0e42\u0e25\u0e22\u0e35";
        
        // จัดรูปแบบชื่อ Instagram ให้ขึ้นต้นด้วย @ เสมอ
        if (instagram != null && !instagram.trim().isEmpty()) {
            String ig = instagram.trim();
            this.instagram = ig.startsWith("@") ? ig : "@" + ig;
        } else {
            this.instagram = "@arpo_cpe";
        }

        this.profileImage = (profileImage != null) ? profileImage.trim() : "";
        this.password = password;
        this.registeredAt = System.currentTimeMillis();
    }

    // [Lecture 4: Method Overloading] Constructor เสริมสำหรับความเข้ากันได้
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String password) {
        this(studentId, fullName, faculty, instagram, "", password);
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

    public boolean checkPassword(String pwd) {
        return this.password != null && this.password.equals(pwd);
    }
}
