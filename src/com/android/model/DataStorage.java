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

    public static synchronized Map<String, StudentUser> loadUsers() {
        Map<String, StudentUser> users = new LinkedHashMap<>();
        if (!USERS_FILE.exists()) {
            return users; // ไม่มีไฟล์ = เริ่มต้นระบบว่างเปล่า (ไม่มีข้อมูลมั่ว)
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(USERS_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("	", -1);
                if (parts.length >= 6) {
                    String studentId = decode(parts[0]);
                    String fullName = decode(parts[1]);
                    String faculty = decode(parts[2]);
                    String instagram = decode(parts[3]);
                    String profileImage = decode(parts[4]);
                    String password = decode(parts[5]);
                    String role = (parts.length >= 8) ? decode(parts[7]) : ("6812247050".equals(studentId) ? "ADMIN" : "STUDENT");

                    StudentUser user = new StudentUser(studentId, fullName, faculty, instagram, profileImage, password, role);
                    users.put(studentId, user);
                }
            }
        } catch (Exception e) {
            System.err.println("[DataStorage] Error loading users: " + e.getMessage());
        }
        return users;
    }

    public static synchronized void saveUsers(Map<String, StudentUser> users) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(USERS_FILE), StandardCharsets.UTF_8))) {
            for (StudentUser u : users.values()) {
                writer.write(encode(u.getStudentId()) + "	" +
                             encode(u.getFullName()) + "	" +
                             encode(u.getFaculty()) + "	" +
                             encode(u.getInstagram()) + "	" +
                             encode(u.getProfileImage()) + "	" +
                             encode(u.getPassword()) + "	" +
                             u.getRegisteredAt() + "	" +
                             encode(u.getRole()));
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("[DataStorage] Error saving users: " + e.getMessage());
        }
    }

    public static synchronized List<LostFoundItem> loadPosts() {
        List<LostFoundItem> posts = new ArrayList<>();
        if (!POSTS_FILE.exists()) {
            return posts; // ไม่มีไฟล์ = เริ่มต้นระบบว่างเปล่า (ไม่มีข้อมูลมั่ว)
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(POSTS_FILE), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("	", -1);
                if (parts.length >= 12) {
                    String id = decode(parts[0]);
                    String title = decode(parts[1]);
                    String type = decode(parts[2]);
                    String category = decode(parts[3]);
                    String location = decode(parts[4]);
                    String description = decode(parts[5]);
                    String photoUrl = decode(parts[6]);
                    String reporterId = decode(parts[7]);
                    String reporterName = decode(parts[8]);
                    String reporterFaculty = decode(parts[9]);
                    String reporterIg = decode(parts[10]);
                    String status = decode(parts[11]);

                    posts.add(new LostFoundItem(
                        id, title, type, category, location, description, photoUrl,
                        reporterId, reporterName, reporterFaculty, reporterIg, status
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[DataStorage] Error loading posts: " + e.getMessage());
        }
        return posts;
    }

    public static synchronized void savePosts(List<LostFoundItem> posts) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(POSTS_FILE), StandardCharsets.UTF_8))) {
            for (LostFoundItem p : posts) {
                writer.write(encode(p.getId()) + "	" +
                             encode(p.getTitle()) + "	" +
                             encode(p.getType()) + "	" +
                             encode(p.getCategory()) + "	" +
                             encode(p.getLocation()) + "	" +
                             encode(p.getDescription()) + "	" +
                             encode(p.getPhotoUrl()) + "	" +
                             encode(p.getReporterId()) + "	" +
                             encode(p.getReporterName()) + "	" +
                             encode(p.getReporterFaculty()) + "	" +
                             encode(p.getReporterIg()) + "	" +
                             encode(p.getStatus()));
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
