package com.android.server;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.UUID;

/**
 * ============================================================================
 * [Lecture 5.2: MVC Architectural Pattern - File & Media Management]
 * ============================================================================
 * 
 * คลาสเครื่องมือช่วยเหลือในการจัดการไฟล์รูปภาพ (Image Storage Helper)
 * 
 * หน้าที่หลัก:
 * 1. รับข้อมูลรูปภาพในรูปแบบ Base64 Data URL จาก Web UI (เช่น data:image/jpeg;base64,...)
 * 2. ถอดรหัส (Decode) ข้อมูล Base64 กลับเป็นไบต์อาร์เรย์ (byte[]) ของไฟล์รูปภาพจริง
 * 3. ตรวจสอบนามสกุลไฟล์ (.jpg, .png, .webp, .gif) จาก MIME Header
 * 4. สร้างชื่อไฟล์สุ่มที่ไม่ซ้ำกัน (UUID + Timestamp) และบันทึกลงโฟลเดอร์ web/uploads/
 * 5. คืนค่า URL สัมพัทธ์ เช่น "/uploads/profile_123.jpg" เพื่อให้เบราว์เซอร์สามารถโหลดไฟล์แบบ Static ได้โดยตรง
 * 
 * @author CPE Student
 * @version 1.0
 */
public class ImageStorageHelper {

    /**
     * ถอดรหัสรูปภาพ Base64 Data URL และบันทึกเป็นไฟล์ลงโฟลเดอร์ uploads บนดิสก์
     * 
     * @param webRootDir ไดเรกทอรีรากของเว็บ (โฟลเดอร์ web)
     * @param base64DataUrl สตริงรูปภาพ Base64 เช่น "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
     * @param prefix คำนำหน้าชื่อไฟล์ เช่น "profile_user" หรือ "item"
     * @return URL สัมพัทธ์สำหรับเรียกดูรูปภาพ เช่น "/uploads/profile_user_123.jpg" หรือค่าเดิมหากไม่ใช่ Base64
     */
    public static String saveBase64Image(File webRootDir, String base64DataUrl, String prefix) {
        // หากไม่ใช่ Base64 Data URL ให้คืนค่าสตริงเดิม (เช่น กรณีเป็นพาธไฟล์เดิมอยู่แล้ว)
        if (base64DataUrl == null || !base64DataUrl.startsWith("data:image/")) {
            return base64DataUrl != null ? base64DataUrl : "";
        }

        try {
            // ค้นหาตำแหน่งเครื่องหมายจุลภาค (,) ที่คั่นระหว่าง Header และข้อมูลเนื้อหา Base64
            int commaIdx = base64DataUrl.indexOf(',');
            if (commaIdx == -1) return "";

            String header = base64DataUrl.substring(0, commaIdx);
            String base64Content = base64DataUrl.substring(commaIdx + 1).trim();

            // ตรวจสอบนามสกุลไฟล์จาก MIME Type ในส่วน Header
            String ext = ".jpg";
            if (header.contains("image/png")) {
                ext = ".png";
            } else if (header.contains("image/webp")) {
                ext = ".webp";
            } else if (header.contains("image/gif")) {
                ext = ".gif";
            }

            // ถอดรหัส Base64 เป็น Array ของ Byte
            byte[] imageBytes = Base64.getDecoder().decode(base64Content);

            // ตรวจสอบและสร้างโฟลเดอร์ uploads หากยังไม่มี
            File uploadsDir = new File(webRootDir, "uploads");
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            // ตั้งชื่อไฟล์รูปภาพที่ไม่ซ้ำกันโดยใช้ timestamp และตัวเลขสุ่ม UUID
            String fileName = prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + ext;
            File targetFile = new File(uploadsDir, fileName);

            // เขียนไบต์ของรูปภาพลงไฟล์จริง
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(imageBytes);
            }

            System.out.println("[ImageStorageHelper] บันทึกไฟล์รูปภาพสำเร็จ: " + fileName + " (" + (imageBytes.length / 1024) + " KB)");
            return "/uploads/" + fileName;

        } catch (Exception e) {
            System.err.println("[ImageStorageHelper] เกิดข้อผิดพลาดในการบันทึกรูปภาพ: " + e.getMessage());
            return "";
        }
    }
}
