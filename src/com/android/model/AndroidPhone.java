package com.android.model;

import com.android.model.apps.AndroidApplication;
import com.android.model.os.AndroidOS;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [Lecture 4: OOP Concepts - Inheritance & Polymorphism]
 *
 * คลาสรูปธรรมของสมาร์ตโฟนระบบแอนดรอยด์ สืบทอดจาก SmartPhone
 * มีการ Override เมธอด powerOn() และ powerOff()
 */
public class AndroidPhone extends SmartPhone {
    private final AndroidOS os;
    private final Map<String, AndroidApplication> installedApps;
    private AndroidApplication currentRunningApp;
    private final List<String> recentAppIds;

    public AndroidPhone(String brand, String model, String serialNumber,
                        int batteryMah, String resolution, double screenInches,
                        int storageMb, int cameraMp, String androidVersion, int apiLevel) {
        super(brand, model, serialNumber, batteryMah, resolution, screenInches, storageMb, cameraMp);
        this.os = new AndroidOS(androidVersion, apiLevel);
        this.installedApps = new LinkedHashMap<>();
        this.currentRunningApp = null;
        this.recentAppIds = new ArrayList<>();
        this.isPoweredOn = true;
    }

    @Override
    public boolean powerOn() {
        if (battery.isDepleted()) {
            return false;
        }
        this.isPoweredOn = true;
        this.display.turnOn();
        os.getNotificationManager().postNotification(
            "System Started",
            "Welcome to " + getModel() + " running " + os.getVersionName(),
            "Android System"
        );
        return true;
    }

    @Override
    public boolean powerOff() {
        if (currentRunningApp != null) {
            closeCurrentApp();
        }
        this.display.turnOff();
        this.isPoweredOn = false;
        return true;
    }

    public void togglePower() {
        if (isPoweredOn) {
            powerOff();
        } else {
            powerOn();
        }
    }

    public synchronized boolean installApp(AndroidApplication app) {
        if (installedApps.containsKey(app.getAppId())) {
            return false;
        }
        installedApps.put(app.getAppId(), app);
        storage.saveFile(app.getAppId() + ".apk", app.getSizeMb(), "application/vnd.android.package-archive");
        os.getNotificationManager().postNotification(
            "App Installed",
            app.getAppName() + " (" + app.getVersion() + ") successfully installed.",
            "Package Manager"
        );
        return true;
    }

    public synchronized boolean launchApp(String appId) {
        if (!isPoweredOn) return false;
        AndroidApplication targetApp = installedApps.get(appId);
        if (targetApp == null) return false;

        if (currentRunningApp != null && !currentRunningApp.getAppId().equals(appId)) {
            currentRunningApp.onPause();
            currentRunningApp.onStop();
        }

        targetApp.onCreate();
        targetApp.onStart();
        targetApp.onResume();
        this.currentRunningApp = targetApp;
        recentAppIds.remove(appId);
        recentAppIds.add(0, appId);
        return true;
    }

    public synchronized void closeCurrentApp() {
        if (currentRunningApp != null) {
            currentRunningApp.onPause();
            currentRunningApp.onStop();
            currentRunningApp.onDestroy();
            currentRunningApp = null;
        }
    }

    public void pressHomeButton() {
        if (!isPoweredOn) return;
        if (currentRunningApp != null) {
            currentRunningApp.onPause();
            currentRunningApp.onStop();
            currentRunningApp = null;
        }
    }

    public void pressBackButton() {
        if (!isPoweredOn) return;
        if (currentRunningApp != null) {
            closeCurrentApp();
        }
    }

    public AndroidApplication.ActionResult executeAppAction(String action, Map<String, Object> params) {
        if (!isPoweredOn) {
            return new AndroidApplication.ActionResult(false, "Device is powered off", null);
        }
        if (currentRunningApp == null) {
            return new AndroidApplication.ActionResult(false, "No app currently running", null);
        }
        return currentRunningApp.executeAction(action, params);
    }

    public AndroidOS getOs() {
        return os;
    }

    public Map<String, AndroidApplication> getInstalledApps() {
        return installedApps;
    }

    public AndroidApplication getCurrentRunningApp() {
        return currentRunningApp;
    }
}
