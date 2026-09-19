package com.android.server;

import com.android.controller.AuthController;
import com.android.controller.PhoneController;
import com.android.model.AndroidPhone;
import com.android.model.LostFoundItem;
import com.android.model.StudentUser;
import com.android.model.apps.AndroidApplication;
import com.android.model.apps.PsruLostFoundApp;
import com.android.model.hardware.Storage;
import com.android.model.os.Notification;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [Lecture 5.2: MVC Architectural Pattern - View & Dispatcher]
 *
 * เว็บเซิร์ฟเวอร์เชื่อมต่อหน้าจอ UI บน Browser กับระบบ PhoneController ในภาษา Java
 */
public class AndroidHttpServer {
    private final int port;
    private final PhoneController phoneController;
    private final AndroidPhone phone;
    private final File webRootDir;
    private HttpServer server;

    public AndroidHttpServer(int port, PhoneController phoneController, File webRootDir) {
        this.port = port;
        this.phoneController = phoneController;
        this.phone = phoneController.getPhoneModel();
        this.webRootDir = webRootDir;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Endpoints ส่งคำสั่งไปยัง Controller
        server.createContext("/api/state", this::handleGetState);
        server.createContext("/api/power", exchange -> {
            phoneController.togglePower();
            sendJsonResponse(exchange, 200, Map.of("success", true, "isPoweredOn", phone.isPoweredOn()));
        });
        server.createContext("/api/volume/up", exchange -> {
            phoneController.volumeUp();
            sendJsonResponse(exchange, 200, Map.of("success", true, "volume", phone.getVolume()));
        });
        server.createContext("/api/volume/down", exchange -> {
            phoneController.volumeDown();
            sendJsonResponse(exchange, 200, Map.of("success", true, "volume", phone.getVolume()));
        });
        server.createContext("/api/battery/charge", exchange -> {
            sendJsonResponse(exchange, 200, Map.of("success", true, "level", 100));
        });
        server.createContext("/api/home", exchange -> {
            phoneController.pressHome();
            sendJsonResponse(exchange, 200, Map.of("success", true));
        });
        server.createContext("/api/back", exchange -> {
            phoneController.pressBack();
            sendJsonResponse(exchange, 200, Map.of("success", true));
        });
        server.createContext("/api/app/launch", this::handleLaunchApp);
        server.createContext("/api/app/close", exchange -> {
            phoneController.closeCurrentApp();
            sendJsonResponse(exchange, 200, Map.of("success", true));
        });
        server.createContext("/api/app/action", this::handleAppAction);
        server.createContext("/api/database", this::handleGetDatabase);

        // ให้บริการไฟล์ Static Web (HTML, CSS, JS, Images)
        server.createContext("/", this::handleStaticFile);

        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleGetState(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        Map<String, Object> state = new LinkedHashMap<>();

        // Device
        Map<String, Object> device = new LinkedHashMap<>();
        device.put("brand", phone.getBrand());
        device.put("model", phone.getModel());
        device.put("serialNumber", phone.getSerialNumber());
        device.put("deviceType", phone.getDeviceType());
        device.put("isPoweredOn", phone.isPoweredOn());
        device.put("volume", phone.getVolume());
        state.put("device", device);

        // Battery
        Map<String, Object> battery = new LinkedHashMap<>();
        battery.put("level", 100);
        battery.put("capacityMah", phone.getBattery().getCapacityMah());
        battery.put("isCharging", true);
        battery.put("temperatureCelsius", 26);
        battery.put("isCritical", false);
        battery.put("isInfinite", true);
        state.put("battery", battery);

        // Display
        Map<String, Object> display = new LinkedHashMap<>();
        display.put("resolution", phone.getDisplay().getResolution());
        display.put("diagonalInches", phone.getDisplay().getDiagonalInches());
        display.put("brightness", phone.getDisplay().getBrightness());
        display.put("isScreenOn", phone.getDisplay().isScreenOn());
        display.put("refreshRateHz", phone.getDisplay().getRefreshRateHz());
        state.put("display", display);

        // Storage
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("totalMb", phone.getStorage().getTotalCapacityMb());
        storage.put("usedMb", phone.getStorage().getUsedCapacityMb());
        storage.put("freeMb", phone.getStorage().getFreeSpaceMb());
        storage.put("usedPercent", Math.round(phone.getStorage().getUsedPercentage()));
        List<Map<String, Object>> filesList = new ArrayList<>();
        for (Storage.StoredFile f : phone.getStorage().getFiles()) {
            filesList.add(Map.of(
                "fileName", f.getFileName(),
                "sizeMb", f.getSizeMb(),
                "fileType", f.getFileType(),
                "createdAt", f.getCreatedAt()
            ));
        }
        storage.put("files", filesList);
        state.put("storage", storage);

        // Network
        Map<String, Object> net = new LinkedHashMap<>();
        net.put("isConnected", phone.getNetworkAdapter().isConnected());
        net.put("connectionType", phone.getNetworkAdapter().getConnectionType());
        net.put("signalStrength", phone.getNetworkAdapter().getSignalStrength());
        net.put("wifiEnabled", phone.getNetworkAdapter().isWifiEnabled());
        net.put("cellularEnabled", phone.getNetworkAdapter().isCellularEnabled());
        state.put("network", net);

        // OS
        Map<String, Object> osMap = new LinkedHashMap<>();
        osMap.put("versionName", phone.getOs().getVersionName());
        osMap.put("apiLevel", phone.getOs().getApiLevel());
        osMap.put("kernel", phone.getOs().getKernelVersion());
        osMap.put("uptimeSeconds", phone.getOs().getUptimeSeconds());
        state.put("os", osMap);

        // Notifications
        List<Map<String, Object>> notifs = new ArrayList<>();
        for (Notification n : phone.getOs().getNotificationManager().getActiveNotifications()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", n.getId());
            item.put("title", n.getTitle());
            item.put("message", n.getMessage());
            item.put("appSource", n.getAppSource());
            item.put("timestamp", n.getTimestamp());
            notifs.add(item);
        }
        state.put("notifications", notifs);

        // Installed Apps
        List<Map<String, Object>> appList = new ArrayList<>();
        for (AndroidApplication app : phone.getInstalledApps().values()) {
            appList.add(Map.of(
                "appId", app.getAppId(),
                "appName", app.getAppName(),
                "icon", app.getIcon(),
                "version", app.getVersion(),
                "sizeMb", app.getSizeMb(),
                "state", app.getState().name()
            ));
        }
        state.put("installedApps", appList);

        // Running App
        AndroidApplication cur = phone.getCurrentRunningApp();
        if (cur != null) {
            Map<String, Object> running = new LinkedHashMap<>();
            running.put("appId", cur.getAppId());
            running.put("appName", cur.getAppName());
            running.put("icon", cur.getIcon());
            running.put("state", cur.getState().name());
            running.put("appData", cur.getAppData());
            state.put("runningApp", running);
        } else {
            state.put("runningApp", null);
        }

        // OOP Info
        Map<String, String> oopInfo = new LinkedHashMap<>();
        oopInfo.put("lecture3", "Classes & Objects: Battery, Display, StudentUser");
        oopInfo.put("lecture4", "OOP 4 Pillars: Abstraction (Device), Inheritance (SmartPhone), Polymorphism (AndroidPhone), Encapsulation");
        oopInfo.put("lecture5_1", "Event-driven: PhoneEventListener, AuthEventListener");
        oopInfo.put("lecture5_2", "MVC Pattern: Model (com.android.model), View (web), Controller (com.android.controller)");
        oopInfo.put("lecture6", "Multitasking: PhoneBackgroundService running on a Thread (Runnable)");
        state.put("oopInfo", oopInfo);

        sendJsonResponse(exchange, 200, state);
    }

    private void handleLaunchApp(HttpExchange exchange) throws IOException {
        Map<String, String> body = parseRequestBody(exchange);
        String appId = body.get("appId");
        if (appId == null || appId.isEmpty()) {
            sendJsonResponse(exchange, 400, Map.of("success", false, "error", "Missing appId"));
            return;
        }
        boolean ok = phoneController.launchApp(appId);
        sendJsonResponse(exchange, 200, Map.of("success", ok, "appId", appId));
    }

    private void handleAppAction(HttpExchange exchange) throws IOException {
        Map<String, String> body = parseRequestBody(exchange);
        String action = body.get("action");
        if (action == null) {
            sendJsonResponse(exchange, 400, Map.of("success", false, "error", "Missing action"));
            return;
        }
        Map<String, Object> params = new HashMap<>(body);

        // จัดการบันทึกรูปภาพ Base64 ลงเป็นไฟล์จริงใน web/uploads
        if (params.containsKey("profileImage")) {
            String rawImg = String.valueOf(params.get("profileImage"));
            if (rawImg.startsWith("data:image/")) {
                String studentId = params.containsKey("studentId") ? String.valueOf(params.get("studentId")) : "user";
                String savedUrl = ImageStorageHelper.saveBase64Image(webRootDir, rawImg, "profile_" + studentId);
                if (!savedUrl.isEmpty()) {
                    params.put("profileImage", savedUrl);
                    phone.getStorage().saveFile(new File(savedUrl).getName(), 2, "image/jpeg");
                }
            }
        }

        if (params.containsKey("photoUrl")) {
            String rawImg = String.valueOf(params.get("photoUrl"));
            if (rawImg.startsWith("data:image/")) {
                String savedUrl = ImageStorageHelper.saveBase64Image(webRootDir, rawImg, "item");
                if (!savedUrl.isEmpty()) {
                    params.put("photoUrl", savedUrl);
                    phone.getStorage().saveFile(new File(savedUrl).getName(), 3, "image/jpeg");
                }
            }
        }

        AndroidApplication.ActionResult result = phone.executeAppAction(action, params);
        sendJsonResponse(exchange, 200, Map.of(
            "success", result.isSuccess(),
            "message", result.message() != null ? result.message() : "",
            "data", result.getData() != null ? result.getData() : Map.of()
        ));
    }

    private void handleStaticFile(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        File file = new File(webRootDir, path.substring(1));
        if (!file.exists() || file.isDirectory()) {
            String notFound = "404 Not Found";
            exchange.sendResponseHeaders(404, notFound.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound.getBytes());
            }
            return;
        }

        String mime = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.sendResponseHeaders(200, file.length());

        try (InputStream is = new FileInputStream(file); OutputStream os = exchange.getResponseBody()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private void sendJsonResponse(HttpExchange exchange, int status, Object data) throws IOException {
        String json = JsonHelper.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return parseSimpleJson(raw);
        }
    }

    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        String s = json.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);

        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escape = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                current.append(c);
                escape = false;
            } else if (c == '\\') {
                current.append(c);
                escape = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                pairs.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            pairs.add(current.toString());
        }

        for (String pair : pairs) {
            int colonIdx = -1;
            boolean inKeyQuotes = false;
            boolean esc = false;
            for (int i = 0; i < pair.length(); i++) {
                char c = pair.charAt(i);
                if (esc) {
                    esc = false;
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    inKeyQuotes = !inKeyQuotes;
                } else if (c == ':' && !inKeyQuotes) {
                    colonIdx = i;
                    break;
                }
            }

            if (colonIdx != -1) {
                String k = unquoteAndUnescape(pair.substring(0, colonIdx).trim());
                String v = unquoteAndUnescape(pair.substring(colonIdx + 1).trim());
                map.put(k, v);
            }
        }
        return map;
    }

    private String unquoteAndUnescape(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 'n' -> { sb.append('\n'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'u' -> {
                        if (i + 5 < s.length()) {
                            try {
                                String hex = s.substring(i + 2, i + 6);
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 5;
                            } catch (Exception e) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                    }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void handleGetDatabase(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        Map<String, Object> db = new LinkedHashMap<>();

        // 1. Users Table (In-Memory Map in AuthController)
        List<Map<String, Object>> usersList = new ArrayList<>();
        AndroidApplication psruApp = phone.getInstalledApps().get("com.psru.lostfound");
        if (psruApp instanceof PsruLostFoundApp lostFoundApp) {
            AuthController auth = lostFoundApp.getAuthController();
            for (StudentUser u : auth.getAllUsers()) {
                Map<String, Object> um = new LinkedHashMap<>();
                um.put("studentId", u.getStudentId());
                um.put("fullName", u.getFullName());
                um.put("faculty", u.getFaculty());
                um.put("instagram", u.getInstagram());
                um.put("hasProfilePhoto", u.getProfileImage() != null && !u.getProfileImage().isEmpty());
                um.put("registeredAt", u.getRegisteredAt());
                usersList.add(um);
            }
        }
        db.put("users", usersList);
        db.put("totalUsers", usersList.size());

        // 2. Lost & Found Items Table (In-Memory List in PsruLostFoundApp)
        List<Map<String, Object>> postsList = new ArrayList<>();
        if (psruApp instanceof PsruLostFoundApp lostFoundApp) {
            for (LostFoundItem item : lostFoundApp.getItemList()) {
                postsList.add(item.toMap());
            }
        }
        db.put("posts", postsList);
        db.put("totalPosts", postsList.size());

        // 3. Storage Files Table (Simulated Flash Memory in Storage)
        List<Map<String, Object>> storageList = new ArrayList<>();
        for (Storage.StoredFile f : phone.getStorage().getFiles()) {
            storageList.add(Map.of(
                "fileName", f.getFileName(),
                "sizeMb", f.getSizeMb(),
                "fileType", f.getFileType(),
                "createdAt", f.getCreatedAt()
            ));
        }
        db.put("storageFiles", storageList);
        db.put("totalStorageFiles", storageList.size());

        // 4. OOP Metadata
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("storageEngine", "In-Memory Object Database (Java Heap Collections)");
        meta.put("usersCollection", "Map<String, StudentUser> userDatabase (AuthController)");
        meta.put("postsCollection", "CopyOnWriteArrayList<LostFoundItem> itemList (PsruLostFoundApp)");
        meta.put("hardwareStorage", "Storage.java (Simulated Flash Storage)");
        meta.put("timestamp", System.currentTimeMillis());
        db.put("metadata", meta);

        sendJsonResponse(exchange, 200, db);
    }
}
