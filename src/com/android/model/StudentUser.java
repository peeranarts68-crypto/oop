package com.android.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ============================================================================
 * [Lecture 3: OOP Class and Object]
 * [Lecture 4: OOP Concepts - Encapsulation (การห่อหุ้ม)]
 * ============================================================================
 * 
 * คลาส Model สำหรับแทนข้อมูลของนักศึกษาและผู้ใช้งานระบบ PSRU Lost & Found
 * 
 * หลักการ OOP ที่นำมาใช้:
 * 1. Encapsulation: ตัวแปรเป็น private ทั้งหมด เพื่อป้องกันการเข้าถึงโดยตรงจากภายนอก
 *    และกำหนด Getter/Setter พร้อมการตรวจสอบความถูกต้อง (Validation)
 * 2. Overloaded Constructors: มี Constructor หลายรูปแบบเพื่อความยืดหยุ่นในการสร้าง Object
 * 3. Role-Based Access Control (RBAC): แยกสิทธิ์ "ADMIN" และ "STUDENT"
 * 
 * @author CPE Student
 * @version 1.0
 */
public class StudentUser {

    // --- คุณลักษณะของผู้ใช้งาน (Private Attributes / Encapsulation) ---

    /** รหัสนักศึกษา 10 หลัก เช่น 6812247050 */
    private final String studentId;

    /** ชื่อ - นามสกุลจริงของนักศึกษา */
    private final String fullName;

    /** คณะที่สังกัด เช่น คณะวิศวกรรมศาสตร์และเทคโนโลยีอุตสาหกรรม */
    private final String faculty;

    /** ช่องทางติดต่อ Instagram (IG) เช่น @s.peeranart */
    private String instagram;

    /** ช่องทางติดต่อ LINE ID เช่น peeranart.s */
    private String line;

    /** ที่อยู่ไฟล์รูปโปรไฟล์ (URL หรือ Local Path เช่น /uploads/profile_...jpg) */
    private String profileImage;

    /** รหัสผ่านสำหรับเข้าสู่ระบบ */
    private String password;

    /** บทบาทหรือสิทธิ์การใช้งาน ("ADMIN" หรือ "STUDENT") */
    private String role;

    /** เวลาที่ลงทะเบียนเข้าสู่ระบบ (Epoch Timestamp มิลลิวินาที) */
    private final long registeredAt;

    /**
     * [Lecture 3: Constructor เต็มรูปแบบ 8 พารามิเตอร์]
     * 
     * @param studentId รหัสนักศึกษา 10 หลัก
     * @param fullName ชื่อ-นามสกุล
     * @param faculty คณะสังกัด
     * @param instagram ช่องทางติดต่อ Instagram
     * @param line ช่องทางติดต่อ Line ID
     * @param profileImage รูปภาพโปรไฟล์
     * @param password รหัสผ่าน
     * @param role สิทธิ์ผู้ใช้งาน ("ADMIN" หรือ "STUDENT")
     */
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String line, String profileImage, String password, String role) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.faculty = (faculty != null && !faculty.trim().isEmpty()) ? faculty : "คณะวิศวกรรมศาสตร์และเทคโนโลยีอุตสาหกรรม";
        
        // จัดรูปแบบชื่อ Instagram ให้มีเครื่องหมาย @ นำหน้าเสมอ
        if (instagram != null && !instagram.trim().isEmpty()) {
            String ig = instagram.trim();
            this.instagram = ig.startsWith("@") ? ig : "@" + ig;
        } else {
            this.instagram = "@arpo_cpe";
        }

        this.line = (line != null) ? line.trim() : "";
        this.profileImage = (profileImage != null) ? profileImage.trim() : "";
        this.password = password;
        
        // กำหนดสิทธิ์ Super Admin ถาวรให้รหัสนักศึกษา 6812247050 และ 6812247020
        if ("6812247050".equals(studentId) || "6812247020".equals(studentId)) {
            this.role = "ADMIN";
        } else {
            this.role = (role != null && !role.trim().isEmpty()) ? role.trim().toUpperCase() : "STUDENT";
        }
        
        this.registeredAt = System.currentTimeMillis();
    }

    /**
     * [Lecture 4: Method Overloading] Constructor 7 พารามิเตอร์ (มี line)
     */
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String line, String profileImage, String password) {
        this(studentId, fullName, faculty, instagram, line, profileImage, password, ("6812247050".equals(studentId) || "6812247020".equals(studentId)) ? "ADMIN" : "STUDENT");
    }

    /**
     * [Lecture 4: Method Overloading] Constructor 6 พารามิเตอร์
     */
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String profileImage, String password) {
        this(studentId, fullName, faculty, instagram, "", profileImage, password);
    }

    /**
     * [Lecture 4: Method Overloading] Constructor พื้นฐาน 5 พารามิเตอร์
     */
    public StudentUser(String studentId, String fullName, String faculty, String instagram, String password) {
        this(studentId, fullName, faculty, instagram, "", "", password);
    }

    // ============================================================================
    // Getter & Setter Methods (Encapsulation)
    // ============================================================================

    /** อ่านรหัสนักศึกษา */
    public String getStudentId() {
        return studentId;
    }

    /** อ่านชื่อ-นามสกุล */
    public String getFullName() {
        return fullName;
    }

    /** อ่านคณะสังกัด */
    public String getFaculty() {
        return faculty;
    }

    /** อ่านบัญชี Instagram */
    public String getInstagram() {
        return instagram;
    }

    /** กำหนดบัญชี Instagram พร้อมใส่ @ นำหน้าอัตโนมัติ */
    public void setInstagram(String instagram) {
        if (instagram != null && !instagram.trim().isEmpty()) {
            String ig = instagram.trim();
            this.instagram = ig.startsWith("@") ? ig : "@" + ig;
        }
    }

    /** อ่านที่อยู่รูปโปรไฟล์ */
    public String getProfileImage() {
        return profileImage;
    }

    /** กำหนดที่อยู่รูปโปรไฟล์ */
    public void setProfileImage(String profileImage) {
        this.profileImage = (profileImage != null) ? profileImage.trim() : "";
    }

    /** อ่านไอดี LINE */
    public String getLine() {
        return line != null ? line : "";
    }

    /** กำหนดไอดี LINE */
    public void setLine(String line) {
        this.line = (line != null) ? line.trim() : "";
    }

    /** เมธอด getPhone เพื่อความเข้ากันได้ย้อนหลัง (คืนค่า Instagram) */
    public String getPhone() {
        return instagram;
    }

    /** อ่านเวลาที่ลงทะเบียน */
    public long getRegisteredAt() {
        return registeredAt;
    }

    /** อ่านรหัสผ่าน */
    public String getPassword() {
        return password;
    }

    /** เปลี่ยนรหัสผ่านใหม่ */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * ตรวจสอบความถูกต้องของรหัสผ่าน
     * @param pwd รหัสผ่านที่รับเข้ามาตรวจสอบ
     * @return true ถ้ารหัสผ่านตรงกัน
     */
    public boolean checkPassword(String pwd) {
        return this.password != null && this.password.equals(pwd);
    }

    // ============================================================================
    // สิทธิ์การใช้งาน (Role-Based Access Control)
    // ============================================================================

    /**
     * อ่านสิทธิ์ผู้ใช้งาน ("ADMIN" หรือ "STUDENT")
     * @return บทบาทผู้ใช้งาน
     */
    public String getRole() {
        if ("6812247050".equals(studentId) || "6812247020".equals(studentId)) {
            return "ADMIN";
        }
        return (role != null && !role.trim().isEmpty()) ? role.trim().toUpperCase() : "STUDENT";
    }

    /**
     * กำหนดสิทธิ์ผู้ใช้งาน (ยกเว้นรหัส 6812247050 และ 6812247020 ที่เป็น ADMIN ถาวร)
     * @param role สิทธิ์เป้าหมาย
     */
    public void setRole(String role) {
        if ("6812247050".equals(studentId) || "6812247020".equals(studentId)) {
            this.role = "ADMIN";
        } else {
            this.role = (role != null) ? role.trim().toUpperCase() : "STUDENT";
        }
    }

    /**
     * ตรวจสอบว่าผู้ใช้งานนี้มีสิทธิ์แอดมินหรือไม่
     * @return true ถ้าเป็น Admin
     */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getRole()) || "6812247050".equals(studentId) || "6812247020".equals(studentId);
    }

    /**
     * แปลง Object เป็น Map เพื่อส่งออกในรูปแบบ JSON ไปยัง Web UI
     * @return ข้อมูลในรูปแบบ Map<String, Object>
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
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
