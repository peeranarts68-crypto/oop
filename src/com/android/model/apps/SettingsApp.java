package com.android.model.apps;

import com.android.model.AndroidPhone;
import java.util.HashMap;
import java.util.Map;

/**
 * [Lecture 4: OOP Concepts - Inheritance & Polymorphism]
 *
 * คลาสแอปพลิเคชันการตั้งค่าระบบ (Settings Application)
 * - Inheritance: สืบทอดคุณสมบัติมาจากคลาสแม่ AndroidApplication
 * - ควบคุมและจัดการชิ้นส่วนฮาร์ดแวร์ของมือถือ (Wi-Fi, 5G, ความสว่าง, ระดับเสียง, การชาร์จ)
 */
public class SettingsApp extends AndroidApplication {
    private final AndroidPhone phone;

    public SettingsApp(AndroidPhone phone) {
        super("com.android.settings", "Settings", "settings", "15.0.0", 30);
        this.phone = phone;
    }

    /**
     * เมธอดประมวลผลคำสั่งของแอปการตั้งค่า (Polymorphism)
     * รองรับ TOGGLE_WIFI, TOGGLE_CELLULAR, SET_BRIGHTNESS, SET_VOLUME, CHARGE_BATTERY
     */
    @Override
    public ActionResult executeAction(String action, Map<String, Object> params) {
        if ("TOGGLE_WIFI".equalsIgnoreCase(action)) {
            phone.getNetworkAdapter().toggleWifi();
            boolean wifi = phone.getNetworkAdapter().isWifiEnabled();
            phone.getOs().getNotificationManager().postNotification(
                "Network Status",
                "Wi-Fi is now " + (wifi ? "Connected" : "Disconnected"),
                "Settings"
            );
            return new ActionResult(true, "Wi-Fi toggled", wifi);

        } else if ("TOGGLE_CELLULAR".equalsIgnoreCase(action)) {
            phone.getNetworkAdapter().toggleCellular();
            boolean cell = phone.getNetworkAdapter().isCellularEnabled();
            return new ActionResult(true, "Cellular toggled", cell);

        } else if ("SET_BRIGHTNESS".equalsIgnoreCase(action)) {
            if (params != null && params.containsKey("brightness")) {
                int b = Integer.parseInt(params.get("brightness").toString());
                phone.getDisplay().setBrightness(b);
                return new ActionResult(true, "Brightness set to " + b + "%", b);
            }
        } else if ("SET_VOLUME".equalsIgnoreCase(action)) {
            if (params != null && params.containsKey("volume")) {
                int v = Integer.parseInt(params.get("volume").toString());
                phone.setVolume(v);
                return new ActionResult(true, "Volume set to " + v + "%", v);
            }
        } else if ("CHARGE_BATTERY".equalsIgnoreCase(action)) {
            int amount = (params != null && params.containsKey("amount")) ? Integer.parseInt(params.get("amount").toString()) : 15;
            phone.getBattery().charge(amount);
            phone.getOs().getNotificationManager().postNotification(
                "Charging",
                "Battery charged +" + amount + "%. Current: " + phone.getBattery().getLevel() + "%",
                "System Power"
            );
            return new ActionResult(true, "Battery charged +" + amount + "%", phone.getBattery().getLevel());
        }
        return new ActionResult(false, "Unknown action for SettingsApp: " + action, null);
    }

    @Override
    public Object getAppData() {
        Map<String, Object> data = new HashMap<>();
        data.put("wifiEnabled", phone.getNetworkAdapter().isWifiEnabled());
        data.put("cellularEnabled", phone.getNetworkAdapter().isCellularEnabled());
        data.put("brightness", phone.getDisplay().getBrightness());
        data.put("volume", phone.getVolume());
        data.put("batteryLevel", phone.getBattery().getLevel());
        data.put("batteryCharging", phone.getBattery().isCharging());
        data.put("storageUsedPercent", phone.getStorage().getUsedPercentage());
        data.put("androidVersion", phone.getOs().getVersionName());
        return data;
    }
}
