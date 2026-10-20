package com.android.model.hardware;

/**
 * [Lecture 4: OOP Concepts - Encapsulation & Interface Implementation]
 *
 * คลาสรูปธรรมที่ทำการ Implement จากอินเทอร์เฟซ NetworkAdapter
 * จัดการสถานะการเปิด/ปิดสัญญาณ Wi-Fi และเครือข่ายมือถือ (5G Cellular)
 */
public class DefaultNetworkAdapter implements NetworkAdapter {
    private boolean wifiEnabled;       // สถานะเปิดใช้งาน Wi-Fi
    private boolean cellularEnabled;   // สถานะเปิดใช้งาน 5G Cellular
    private int wifiSignal;            // ความแรงสัญญาณ Wi-Fi (%)
    private int cellularSignal;        // ความแรงสัญญาณมือถือ (%)

    public DefaultNetworkAdapter() {
        this.wifiEnabled = true;
        this.cellularEnabled = true;
        this.wifiSignal = 85;
        this.cellularSignal = 92;
    }

    @Override
    public boolean isConnected() {
        return wifiEnabled || cellularEnabled;
    }

    @Override
    public String getConnectionType() {
        if (wifiEnabled) return "Wi-Fi (5GHz)";
        if (cellularEnabled) return "5G Mobile";
        return "No Internet Connection";
    }

    @Override
    public int getSignalStrength() {
        if (wifiEnabled) return wifiSignal;
        if (cellularEnabled) return cellularSignal;
        return 0;
    }

    @Override
    public void toggleWifi() {
        this.wifiEnabled = !this.wifiEnabled;
    }

    @Override
    public void toggleCellular() {
        this.cellularEnabled = !this.cellularEnabled;
    }

    @Override
    public boolean isWifiEnabled() {
        return wifiEnabled;
    }

    @Override
    public boolean isCellularEnabled() {
        return cellularEnabled;
    }
}
