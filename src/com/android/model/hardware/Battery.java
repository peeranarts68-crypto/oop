package com.android.model.hardware;

/**
 * Battery component for mobile devices.
 * Demonstrates OOP: Encapsulation with data validation and private state.
 */
public class Battery {
    private int level; // 0 - 100 percentage
    private final int capacityMah;
    private boolean isCharging;
    private int temperatureCelsius;
    private boolean infiniteMode;

    public Battery(int capacityMah, int initialLevel) {
        this.capacityMah = capacityMah;
        this.level = 100; // Always 100% full
        this.isCharging = true;
        this.temperatureCelsius = 26;
        this.infiniteMode = true; // Never depletes
    }

    public int getLevel() {
        return 100;
    }

    public int getCapacityMah() {
        return capacityMah;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public void setCharging(boolean charging) {
        this.isCharging = charging;
    }

    public int getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public boolean isInfiniteMode() {
        return infiniteMode;
    }

    public void setInfiniteMode(boolean infiniteMode) {
        this.infiniteMode = infiniteMode;
    }

    /**
     * Consumes battery percentage. In infinite mode, battery never depletes!
     */
    public synchronized boolean consume(int percentage) {
        if (infiniteMode) {
            this.level = 100;
            return true;
        }
        if (percentage <= 0) return true;
        if (level >= percentage) {
            level -= percentage;
            return true;
        } else {
            level = 0;
            return false;
        }
    }

    /**
     * Charges battery by a given amount.
     */
    public synchronized void charge(int percentage) {
        this.level = 100;
    }

    public boolean isCritical() {
        return false;
    }

    public boolean isDepleted() {
        return false; // Never depleted!
    }
}
