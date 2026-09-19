package com.android.model;

import com.android.model.hardware.Battery;
import com.android.model.hardware.Camera;
import com.android.model.hardware.DefaultNetworkAdapter;
import com.android.model.hardware.Display;
import com.android.model.hardware.NetworkAdapter;
import com.android.model.hardware.Storage;

/**
 * [Lecture 4: OOP Concepts - Inheritance & Composition]
 *
 * คลาสสมาร์ตโฟน สืบทอด (Inheritance) มาจาก Device ด้วยคีย์เวิร์ด extends
 * และประกอบด้วย (Composition) ชิ้นส่วนฮาร์ดแวร์ Battery, Display, Storage, Network
 */
public abstract class SmartPhone extends Device {
    protected final Battery battery;
    protected final Display display;
    protected final Storage storage;
    protected final Camera camera;
    protected final NetworkAdapter networkAdapter;
    protected int volume; // ระดับเสียง 0 - 100%

    public SmartPhone(String brand, String model, String serialNumber,
                      int batteryMah, String screenResolution, double screenInches,
                      int storageMb, int cameraMp) {
        super(brand, model, serialNumber); // เรียก Constructor ของคลาสแม่ (Device)
        this.battery = new Battery(batteryMah, 100);
        this.display = new Display(screenResolution, screenInches, 120);
        this.storage = new Storage(storageMb);
        this.camera = new Camera(cameraMp);
        this.networkAdapter = new DefaultNetworkAdapter();
        this.volume = 70;
    }

    public Battery getBattery() {
        return battery;
    }

    public Display getDisplay() {
        return display;
    }

    public Storage getStorage() {
        return storage;
    }

    public Camera getCamera() {
        return camera;
    }

    public NetworkAdapter getNetworkAdapter() {
        return networkAdapter;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
    }

    public void volumeUp() {
        setVolume(this.volume + 10);
    }

    public void volumeDown() {
        setVolume(this.volume - 10);
    }

    @Override
    public String getDeviceType() {
        return "SmartPhone";
    }
}
