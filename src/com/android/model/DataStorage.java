package com.android.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [Lecture 5.2: MVC Model Persistence]
 *
 * คลาสบันทึกและโหลดข้อมูลจากไฟล์ในเครื่อง (data/users.dat, data/posts.dat)
 * เก็บเฉพาะข้อมูลจริงที่ผู้ใช้งานสร้างขึ้นมาเท่านั้น โดยไม่มีการสร้างข้อมูลสุ่มหรือ mock data
 * ข้อมูลแต่ละแถวจะถูกแบ่งออกเป็นแต่ละ Column โดยใช้เครื่องหมาย Tab (\t) เป็นตัวคั่น
 */
public class DataStorage {
    private static final File DATA_DIR = new File("data");
    private static final File USERS_FILE = new File(DATA_DIR, "users.dat");
    private static final File POSTS_FILE = new File(DATA_DIR, "posts.dat");

    static {
        if (!DATA_DIR.exists()) {
            DATA_DIR.mkdirs();
        }
    }

    /**
     * โหลดข้อมูลนักศึกษาทั้งหมดจากไฟล์ data/users.dat
     * โครงสร้างของแต่ละ Column ในไฟล์ users.dat (คั่นด้วย Tab \t):
     * - Column 0: รหัสนักศึกษา 10 หลัก (studentId) เช่น "6812247050"
     * - Column 1: ชื่อ - นามสกุลจริง (fullName) เช่น "พีรนาท สิงห์โต"
     * - Column 2: คณะที่สังกัดในมหาวิทยาลัย (faculty) เช่น "คณะวิศวกรรมศาสตร์และเทคโนโลยีอุตสาหกรรม"
     * - Column 3: บัญชี Instagram ติดต่อ (instagram) เช่น "@s.peeranart"
     * - Column 4: รูปถ่ายโปรไฟล์ส่วนตัว (profileImage)
     * - Column 5: รหัสผ่านเข้าสู่ระบบ (password)
     * - Column 6: เวลาที่ลงทะเบียนเข้าสู่ระบบ (registeredAt: Unix timestamp หน่วย ms)
     * - Column 7: สิทธิ์ผู้ใช้งาน (role: "ADMIN" หรือ "STUDENT")
     * - Column 8: บัญชี Line ID (line: ถ้ามี)
     */
    public static synchronized Map<String, StudentUser> loadUsers() {
        Map<String, StudentUser> users = new LinkedHashMap<>();
        if (!USERS_FILE.exists()) {
            return users; // ไม่มีไฟล์ = เริ่มต้นระบบว่างเปล่า (ไม่มีข้อมูลมั่ว)
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(USERS_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // ข้ามบรรทัดว่าง และบรรทัด Header คำอธิบายคอลัมน์ที่ขึ้นต้นด้วยเครื่องหมาย #
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\t", -1);
                if (parts.length >= 6) {
                    // Column 0: รหัสนักศึกษา 10 หลัก
                    String studentId = decode(parts[0]);

                    // Column 1: ชื่อ - นามสกุลจริง
                    String fullName = decode(parts[1]);

                    // Column 2: คณะสังกัด
                    String faculty = decode(parts[2]);

                    // Column 3: บัญชี Instagram
                    String instagram = decode(parts[3]);

                    // Column 4: ที่อยู่รูปโปรไฟล์
                    String profileImage = decode(parts[4]);

                    // Column 5: รหัสผ่าน
                    String password = decode(parts[5]);

                    // Column 7: สิทธิ์ผู้ใช้งาน (ADMIN หรือ STUDENT)
                    String role = (parts.length >= 8) ? decode(parts[7]) : ("6812247050".equals(studentId) || "6812247020".equals(studentId) ? "ADMIN" : "STUDENT");

                    // Column 8 (ถ้ามี): Line ID
                    String lineContact = (parts.length >= 9) ? decode(parts[8]) : "";

                    StudentUser user = new StudentUser(studentId, fullName, faculty, instagram, lineContact, profileImage, password, role);
                    users.put(studentId, user);
                }
            }
        } catch (Exception e) {
            System.err.println("[DataStorage] Error loading users: " + e.getMessage());
        }
        return users;
    }

    /**
     * บันทึกข้อมูลนักศึกษาทั้งหมดลงในไฟล์ data/users.dat
     * เขียนบรรทัด Header ภาษาไทยระบุชื่อ Column ไว้อย่างชัดเจน
     */
    public static synchronized void saveUsers(Map<String, StudentUser> users) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(USERS_FILE), StandardCharsets.UTF_8))) {
            // เขียนบรรทัด Header อธิบายทุก Column เป็นภาษาไทย (ขึ้นต้นด้วย #)
            writer.write("# [ตารางผู้ใช้งาน / USERS TABLE] Column: 0=รหัสนักศึกษา\t1=ชื่อ-นามสกุล\t2=คณะสังกัด\t3=Instagram\t4=รูปโปรไฟล์\t5=รหัสผ่าน\t6=เวลาลงทะเบียน\t7=สิทธิ์ผู้ใช้งาน\t8=LineID");
            writer.newLine();

            for (StudentUser u : users.values()) {
                writer.write(encode(u.getStudentId()) + "\t" +          // Column 0: รหัสนักศึกษา
                             encode(u.getFullName()) + "\t" +           // Column 1: ชื่อ - นามสกุล
                             encode(u.getFaculty()) + "\t" +            // Column 2: คณะสังกัด
                             encode(u.getInstagram()) + "\t" +          // Column 3: Instagram
                             encode(u.getProfileImage()) + "\t" +       // Column 4: รูปโปรไฟล์
                             encode(u.getPassword()) + "\t" +           // Column 5: รหัสผ่าน
                             u.getRegisteredAt() + "\t" +               // Column 6: เวลาลงทะเบียน (ms)
                             encode(u.getRole()) + "\t" +               // Column 7: สิทธิ์ (ADMIN/STUDENT)
                             encode(u.getLine()));                      // Column 8: Line ID
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("[DataStorage] Error saving users: " + e.getMessage());
        }
    }

    /**
     * โหลดรายการประกาศของหายและแจ้งพบทั้งหมดจากไฟล์ data/posts.dat
     * โครงสร้างของแต่ละ Column ในไฟล์ posts.dat (คั่นด้วย Tab \t):
     * - Column 0: รหัสประกาศเฉพาะ (id) เช่น "ITEM-101"
     * - Column 1: ชื่อสิ่งของที่หายหรือพบ (title) เช่น "iPad Air 5 สีฟ้า"
     * - Column 2: ประเภทประกาศ (type: "LOST" = ตามหาของหาย, "FOUND" = แจ้งพบของ)
     * - Column 3: หมวดหมู่สิ่งของ (category) เช่น "อุปกรณ์อิเล็กทรอนิกส์"
     * - Column 4: สถานที่ที่หายหรือเก็บได้ (location) เช่น "โรงอาหารกลาง ชั้น 1"
     * - Column 5: รายละเอียดและลักษณะเฉพาะ (description) เช่น "เคสสีดำ มีสติกเกอร์แมว"
     * - Column 6: รูปถ่ายสิ่งของ (photoUrl) ในรูปแบบ Data URL หรือ Image Path
     * - Column 7: รหัสนักศึกษาของผู้ลงประกาศ (reporterId) เช่น "6812247050"
     * - Column 8: ชื่อ - นามสกุลของผู้ลงประกาศ (reporterName)
     * - Column 9: คณะสังกัดของผู้ลงประกาศ (reporterFaculty)
     * - Column 10: ช่องทาง Instagram ติดต่อผู้ลงประกาศ (reporterIg) เพื่อส่งคืน
     * - Column 11: สถานะประกาศ (status: "OPEN" = กำลังค้นหา, "RESOLVED" = ส่งคืนสำเร็จแล้ว)
     * - Column 12: รูปถ่ายโปรไฟล์ของผู้ลงประกาศ (reporterPhoto)
     */
    public static synchronized List<LostFoundItem> loadPosts() {
        List<LostFoundItem> posts = new ArrayList<>();
        if (!POSTS_FILE.exists()) {
            return posts; // ไม่มีไฟล์ = เริ่มต้นระบบว่างเปล่า (ไม่มีข้อมูลมั่ว)
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(POSTS_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // ข้ามบรรทัดว่าง และบรรทัด Header คำอธิบายคอลัมน์ที่ขึ้นต้นด้วยเครื่องหมาย #
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\t", -1);
                if (parts.length >= 12) {
                    // Column 0: รหัสประกาศ (Item ID)
                    String id = decode(parts[0]);

                    // Column 1: ชื่อสิ่งของที่หายหรือพบ
                    String title = decode(parts[1]);

                    // Column 2: ประเภทประกาศ ("LOST" หรือ "FOUND")
                    String type = decode(parts[2]);

                    // Column 3: หมวดหมู่สิ่งของ
                    String category = decode(parts[3]);

                    // Column 4: สถานที่ที่หายหรือเก็บได้
                    String location = decode(parts[4]);

                    // Column 5: รายละเอียดและลักษณะเฉพาะของสิ่งของ
                    String description = decode(parts[5]);

                    // Column 6: รูปภาพสิ่งของ (Photo URL)
                    String photoUrl = decode(parts[6]);

                    // Column 7: รหัสนักศึกษาของผู้ลงประกาศ
                    String reporterId = decode(parts[7]);

                    // Column 8: ชื่อ - นามสกุลของผู้ลงประกาศ
                    String reporterName = decode(parts[8]);

                    // Column 9: คณะสังกัดของผู้ลงประกาศ
                    String reporterFaculty = decode(parts[9]);

                    // Column 10: Instagram สำหรับติดต่อส่งคืน
                    String reporterIg = decode(parts[10]);

                    // Column 11: สถานะประกาศ ("OPEN" หรือ "RESOLVED")
                    String status = decode(parts[11]);

                    // Column 12: รูปถ่ายโปรไฟล์ของผู้ลงประกาศ
                    String reporterPhoto = (parts.length >= 13) ? decode(parts[12]) : "";

                    posts.add(new LostFoundItem(
                        id, title, type, category, location, description, photoUrl,
                        reporterId, reporterName, reporterFaculty, reporterIg, reporterPhoto, status
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[DataStorage] Error loading posts: " + e.getMessage());
        }
        return posts;
    }

    /**
     * บันทึกรายการประกาศของหายและแจ้งพบทั้งหมดลงในไฟล์ data/posts.dat
     * เขียนบรรทัด Header ภาษาไทยระบุชื่อ Column ไว้อย่างชัดเจน
     */
    public static synchronized void savePosts(List<LostFoundItem> posts) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(POSTS_FILE), StandardCharsets.UTF_8))) {
            // เขียนบรรทัด Header อธิบายทุก Column เป็นภาษาไทย (ขึ้นต้นด้วย #)
            writer.write("# [ตารางประกาศของหาย / POSTS TABLE] Column: 0=รหัสประกาศ\t1=ชื่อสิ่งของ\t2=ประเภท(LOST/FOUND)\t3=หมวดหมู่\t4=สถานที่\t5=รายละเอียด\t6=รูปสิ่งของ\t7=รหัสผู้แจ้ง\t8=ชื่อผู้แจ้ง\t9=คณะผู้แจ้ง\t10=Instagram\t11=สถานะ(OPEN/RESOLVED)\t12=รูปผู้แจ้ง");
            writer.newLine();

            for (LostFoundItem p : posts) {
                writer.write(encode(p.getId()) + "\t" +               // Column 0: รหัสประกาศ (ID)
                             encode(p.getTitle()) + "\t" +            // Column 1: ชื่อสิ่งของ (Title)
                             encode(p.getType()) + "\t" +             // Column 2: ประเภท (Type)
                             encode(p.getCategory()) + "\t" +         // Column 3: หมวดหมู่ (Category)
                             encode(p.getLocation()) + "\t" +         // Column 4: สถานที่ (Location)
                             encode(p.getDescription()) + "\t" +      // Column 5: รายละเอียด (Description)
                             encode(p.getPhotoUrl()) + "\t" +         // Column 6: รูปภาพสิ่งของ (Photo URL)
                             encode(p.getReporterId()) + "\t" +       // Column 7: รหัสนักศึกษาผู้แจ้ง (Reporter ID)
                             encode(p.getReporterName()) + "\t" +     // Column 8: ชื่อผู้แจ้ง (Reporter Name)
                             encode(p.getReporterFaculty()) + "\t" +  // Column 9: คณะผู้แจ้ง (Reporter Faculty)
                             encode(p.getReporterIg()) + "\t" +       // Column 10: Instagram ผู้แจ้ง
                             encode(p.getStatus()) + "\t" +           // Column 11: สถานะประกาศ (Status)
                             encode(p.getReporterPhoto()));           // Column 12: รูปโปรไฟล์ผู้แจ้ง
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("[DataStorage] Error saving posts: " + e.getMessage());
        }
    }

    private static String encode(String s) {
        if (s == null) return "";
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String decode(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
