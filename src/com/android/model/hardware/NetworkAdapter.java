package com.android.model.hardware;

/**
 * Interface representing hardware network connectivity.
 * Demonstrates OOP: Abstraction & Interface segregation.
 */
public interface NetworkAdapter {
    boolean isConnected();
    String getConnectionType();
    int getSignalStrength(); // 0 to 100%
    void toggleWifi();
    void toggleCellular();
    boolean isWifiEnabled();
    boolean isCellularEnabled();
}
