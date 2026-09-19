package com.android.model.os;

/**
 * Operating System layer representing Android OS.
 * Demonstrates OOP: Encapsulation & Abstraction of OS services.
 */
public class AndroidOS {
    private final String versionName; // e.g. "Android 15 (Vanilla Ice Cream)"
    private final int apiLevel; // e.g. 35
    private final String kernelVersion;
    private final long bootTimeMillis;
    private final NotificationManager notificationManager;

    public AndroidOS(String versionName, int apiLevel) {
        this.versionName = versionName;
        this.apiLevel = apiLevel;
        this.kernelVersion = "Linux 6.6.21-android15-ge91c4d";
        this.bootTimeMillis = System.currentTimeMillis();
        this.notificationManager = new NotificationManager();
    }

    public String getVersionName() {
        return versionName;
    }

    public int getApiLevel() {
        return apiLevel;
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - bootTimeMillis) / 1000;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
}
