package com.android.server;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.UUID;

/**
 * [Lecture 5.2: Image Storage & File Management]
 *
 * คลาสช่วยเหลือในการถอดรหัส Base64 Data URL และบันทึกเป็นไฟล์รูปภาพจริงในโฟลเดอร์ web/uploads
 * เพื่อให้เบราว์เซอร์สามารถโหลดรูปภาพได้อย่างรวดเร็ว ไม่ทำให้ JSON หรือ Memory บวม
 */
public class ImageStorageHelper {

    public static String saveBase64Image(File webRootDir, String base64DataUrl, String prefix) {
        if (base64DataUrl == null || !base64DataUrl.startsWith("data:image/")) {
            return base64DataUrl != null ? base64DataUrl : "";
        }

        try {
            int commaIdx = base64DataUrl.indexOf(',');
            if (commaIdx == -1) return "";

            String header = base64DataUrl.substring(0, commaIdx);
            String base64Content = base64DataUrl.substring(commaIdx + 1).trim();

            String ext = ".jpg";
            if (header.contains("image/png")) {
                ext = ".png";
            } else if (header.contains("image/webp")) {
                ext = ".webp";
            } else if (header.contains("image/gif")) {
                ext = ".gif";
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Content);

            File uploadsDir = new File(webRootDir, "uploads");
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            String fileName = prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + ext;
            File targetFile = new File(uploadsDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(imageBytes);
            }

            System.out.println("[ImageStorageHelper] Saved image: " + fileName + " (" + (imageBytes.length / 1024) + " KB)");
            return "/uploads/" + fileName;

        } catch (Exception e) {
            System.err.println("[ImageStorageHelper] Error saving image: " + e.getMessage());
            return "";
        }
    }
}
