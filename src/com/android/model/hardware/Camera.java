package com.android.model.hardware;

/**
 * [Lecture 3: OOP Class and Object]
 * [Lecture 4: OOP Concepts - Encapsulation]
 *
 * คลาสชิ้นส่วนฮาร์ดแวร์กล้องถ่ายรูป (Camera Hardware Component) ของสมาร์ตโฟน
 * ควบคุมความละเอียดเมกะพิกเซล, ไฟแฟลช, ระดับการซูม (Zoom Level), และการถ่ายภาพ
 */
public class Camera {
    private final int megaPixels;    // ความละเอียดเซนเซอร์กล้อง (MegaPixels) เช่น 50 MP
    private boolean flashEnabled;    // สถานะเปิด/ปิดไฟแฟลช (true = เปิดแฟลช)
    private double zoomLevel;        // ระดับกำลังซูมขยายภาพ (1.0x ถึง 10.0x)
    private int photoCount;          // จำนวนรูปภาพที่ถ่ายไปแล้วทั้งหมด

    public Camera(int megaPixels) {
        this.megaPixels = megaPixels;
        this.flashEnabled = false;
        this.zoomLevel = 1.0;
        this.photoCount = 0;
    }

    public int getMegaPixels() {
        return megaPixels;
    }

    public boolean isFlashEnabled() {
        return flashEnabled;
    }

    public void setFlashEnabled(boolean flashEnabled) {
        this.flashEnabled = flashEnabled;
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = Math.max(1.0, Math.min(10.0, zoomLevel));
    }

    /**
     * เมธอดจำลองการกดชัตเตอร์ถ่ายภาพ
     * สุ่มสร้างชื่อไฟล์และคำนวณขนาดภาพ (MB)
     */
    public synchronized PhotoCaptureResult capturePhoto() {
        photoCount++;
        String filename = "IMG_" + System.currentTimeMillis() + ".jpg";
        int photoSizeMb = 4; // ค่าเฉลี่ยขนาดรูป 4MB สำหรับความละเอียด 50MP
        return new PhotoCaptureResult(filename, photoSizeMb, zoomLevel, flashEnabled);
    }

    public int getPhotoCount() {
        return photoCount;
    }

    /**
     * Inner Class ผลลัพธ์จากการถ่ายภาพ
     */
    public static class PhotoCaptureResult {
        private final String fileName;
        private final int sizeMb;
        private final double zoom;
        private final boolean usedFlash;

        public PhotoCaptureResult(String fileName, int sizeMb, double zoom, boolean usedFlash) {
            this.fileName = fileName;
            this.sizeMb = sizeMb;
            this.zoom = zoom;
            this.usedFlash = usedFlash;
        }

        public String getFileName() { return fileName; }
        public int getSizeMb() { return sizeMb; }
        public double getZoom() { return zoom; }
        public boolean isUsedFlash() { return usedFlash; }
    }
}
