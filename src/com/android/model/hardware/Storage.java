package com.android.model.hardware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * [Lecture 3: OOP Class and Object]
 * [Lecture 4: OOP Concepts - Encapsulation]
 *
 * คลาสชิ้นส่วนฮาร์ดแวร์หน่วยความจำภายใน (Storage Hardware Component)
 * จำลองพื้นที่ความจุหน่วยความจำแฟลชของสมาร์ตโฟน (Internal Flash Storage)
 * มี Inner Class ชื่อ StoredFile สำหรับจำลองอ็อบเจกต์ของไฟล์ที่บันทึกไว้ในเครื่อง
 */
public class Storage {

    /**
     * Inner Class จำลองไฟล์ข้อมูลที่บันทึกลงในหน่วยความจำ
     */
    public static class StoredFile {
        private final String fileName;   // ชื่อไฟล์ เช่น "system.img", "users.dat"
        private final int sizeMb;        // ขนาดของไฟล์ในหน่วยเมกะไบต์ (MB)
        private final String fileType;   // ประเภทไฟล์ เช่น "SYSTEM", "DATA", "image/jpeg"
        private final long createdAt;    // เวลาที่ไฟล์ถูกสร้างขึ้น (Timestamp ms)

        public StoredFile(String fileName, int sizeMb, String fileType) {
            this.fileName = fileName;
            this.sizeMb = sizeMb;
            this.fileType = fileType;
            this.createdAt = System.currentTimeMillis();
        }

        public String getFileName() { return fileName; }
        public int getSizeMb() { return sizeMb; }
        public String getFileType() { return fileType; }
        public long getCreatedAt() { return createdAt; }
    }

    private final int totalCapacityMb;    // ความจุทั้งหมดของหน่วยความจำในเครื่อง (MB)
    private int usedCapacityMb;           // พื้นที่ที่ถูกใช้งานไปแล้ว (MB)
    private final List<StoredFile> files; // รายการไฟล์ที่จัดเก็บไว้ในหน่วยความจำ

    /**
     * คอนสตรักเตอร์กำหนดความจุรวมของพื้นที่จัดเก็บข้อมูล
     * @param totalCapacityMb ความจุรวม เช่น 131072 MB (128 GB)
     */
    public Storage(int totalCapacityMb) {
        this.totalCapacityMb = totalCapacityMb;
        this.usedCapacityMb = 12000;      // จัดสรร ~12GB ให้เป็นพื้นที่พื้นฐานของระบบปฏิบัติการ Android
        this.files = new ArrayList<>();
    }

    /**
     * บันทึกไฟล์ใหม่ลงในหน่วยความจำ
     * ตรวจสอบว่าพื้นที่ว่างคงเหลือเพียงพอหรือไม่ก่อนบันทึก
     */
    public synchronized boolean saveFile(String fileName, int sizeMb, String fileType) {
        if (getFreeSpaceMb() < sizeMb) {
            return false; // พื้นที่ไม่พอ
        }
        files.add(new StoredFile(fileName, sizeMb, fileType));
        usedCapacityMb += sizeMb;
        return true;
    }

    /**
     * ลบไฟล์ออกจากหน่วยความจำ และคืนพื้นที่ว่างกลับมา
     */
    public synchronized boolean deleteFile(String fileName) {
        for (int i = 0; i < files.size(); i++) {
            StoredFile f = files.get(i);
            if (f.getFileName().equalsIgnoreCase(fileName)) {
                usedCapacityMb -= f.getSizeMb();
                files.remove(i);
                return true;
            }
        }
        return false;
    }

    public int getTotalCapacityMb() {
        return totalCapacityMb;
    }

    public int getUsedCapacityMb() {
        return usedCapacityMb;
    }

    public int getFreeSpaceMb() {
        return Math.max(0, totalCapacityMb - usedCapacityMb);
    }

    public int getUsedPercentage() {
        if (totalCapacityMb <= 0) return 0;
        return (int) Math.round(((double) usedCapacityMb / totalCapacityMb) * 100);
    }

    public List<StoredFile> getFiles() {
        return Collections.unmodifiableList(files);
    }
}
