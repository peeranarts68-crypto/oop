package com.android.model.hardware;

/**
 * Concrete implementation of NetworkAdapter.
 * Demonstrates Encapsulation and Interface Implementation.
 */
public class DefaultNetworkAdapter implements NetworkAdapter {
    private boolean wifiEnabled;
    private boolean cellularEnabled;
    private int wifiSignal;
    private int cellularSignal;

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
