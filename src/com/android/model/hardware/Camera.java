package com.android.model.hardware;

/**
 * Camera hardware component.
 * Demonstrates OOP: Encapsulation.
 */
public class Camera {
    private final int megaPixels;
    private boolean flashEnabled;
    private double zoomLevel; // 1.0x to 10.0x
    private int photoCount;

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
     * Captures photo and returns the generated filename and size in MB.
     */
    public synchronized PhotoCaptureResult capturePhoto() {
        photoCount++;
        String filename = "IMG_" + System.currentTimeMillis() + ".jpg";
        int photoSizeMb = 4; // average 4MB per 50MP compressed photo
        return new PhotoCaptureResult(filename, photoSizeMb, zoomLevel, flashEnabled);
    }

    public int getPhotoCount() {
        return photoCount;
    }

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
