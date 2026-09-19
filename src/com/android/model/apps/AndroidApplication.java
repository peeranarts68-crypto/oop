package com.android.model.apps;

import java.util.Map;

/**
 * Abstract base class for all Android Applications.
 * Demonstrates OOP:
 *  - Abstraction: Android lifecycle contract
 *  - Polymorphism: each app implements custom lifecycle and action logic
 */
public abstract class AndroidApplication {
    public enum LifecycleState {
        NOT_INITIALIZED,
        CREATED,
        STARTED,
        RESUMED,
        PAUSED,
        STOPPED,
        DESTROYED
    }

    protected final String appId;
    protected final String appName;
    protected final String icon; // Icon identifier or SVG name
    protected final String version;
    protected final int sizeMb;
    protected LifecycleState state;

    public AndroidApplication(String appId, String appName, String icon, String version, int sizeMb) {
        this.appId = appId;
        this.appName = appName;
        this.icon = icon;
        this.version = version;
        this.sizeMb = sizeMb;
        this.state = LifecycleState.NOT_INITIALIZED;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppName() {
        return appName;
    }

    public String getIcon() {
        return icon;
    }

    public String getVersion() {
        return version;
    }

    public int getSizeMb() {
        return sizeMb;
    }

    public LifecycleState getState() {
        return state;
    }

    // Android Activity Lifecycle methods
    public void onCreate() {
        this.state = LifecycleState.CREATED;
    }

    public void onStart() {
        this.state = LifecycleState.STARTED;
    }

    public void onResume() {
        this.state = LifecycleState.RESUMED;
    }

    public void onPause() {
        this.state = LifecycleState.PAUSED;
    }

    public void onStop() {
        this.state = LifecycleState.STOPPED;
    }

    public void onDestroy() {
        this.state = LifecycleState.DESTROYED;
    }

    /**
     * Polymorphic action dispatch.
     * Subclasses implement their unique app business logic.
     */
    public abstract ActionResult executeAction(String action, Map<String, Object> params);

    /**
     * Returns app-specific state data for the UI.
     */
    public abstract Object getAppData();

    public static class ActionResult {
        private final boolean success;
        private final String message;
        private final Object data;

        public ActionResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public String message() { return message; }
        public Object getData() { return data; }
    }
}
