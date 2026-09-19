package com.android.model.hardware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Storage hardware component simulating internal storage.
 * Demonstrates OOP: Encapsulation with inner class and collection encapsulation.
 */
public class Storage {

    public static class StoredFile {
        private final String fileName;
        private final int sizeMb;
        private final String fileType;
        private final long createdAt;

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

    private final int totalCapacityMb;
    private int usedCapacityMb;
    private final List<StoredFile> files;

    public Storage(int totalCapacityMb) {
        this.totalCapacityMb = totalCapacityMb;
        this.usedCapacityMb = 12000; // ~12GB for Android OS baseline
        this.files = new ArrayList<>();
    }

    public synchronized boolean saveFile(String fileName, int sizeMb, String fileType) {
        if (getFreeSpaceMb() < sizeMb) {
            return false;
        }
        files.add(new StoredFile(fileName, sizeMb, fileType));
        usedCapacityMb += sizeMb;
        return true;
    }

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

    public double getUsedPercentage() {
        return (double) usedCapacityMb / totalCapacityMb * 100.0;
    }

    public List<StoredFile> getFiles() {
        return Collections.unmodifiableList(files);
    }
}
