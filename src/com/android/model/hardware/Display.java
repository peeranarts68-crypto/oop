package com.android.model.hardware;

/**
 * Display hardware component.
 * Demonstrates OOP: Encapsulation.
 */
public class Display {
    private final String resolution; // e.g. "1080x2400"
    private final double diagonalInches; // e.g. 6.7
    private int brightness; // 0 - 100%
    private boolean isScreenOn;
    private int refreshRateHz; // 60, 90, 120

    public Display(String resolution, double diagonalInches, int refreshRateHz) {
        this.resolution = resolution;
        this.diagonalInches = diagonalInches;
        this.brightness = 75;
        this.isScreenOn = true;
        this.refreshRateHz = refreshRateHz;
    }

    public String getResolution() {
        return resolution;
    }

    public double getDiagonalInches() {
        return diagonalInches;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = Math.max(0, Math.min(100, brightness));
    }

    public boolean isScreenOn() {
        return isScreenOn;
    }

    public void turnOn() {
        this.isScreenOn = true;
    }

    public void turnOff() {
        this.isScreenOn = false;
    }

    public void toggleScreen() {
        this.isScreenOn = !this.isScreenOn;
    }

    public int getRefreshRateHz() {
        return refreshRateHz;
    }

    public void setRefreshRateHz(int refreshRateHz) {
        this.refreshRateHz = refreshRateHz;
    }
}
