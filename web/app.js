// Default System Installed Applications
const DEFAULT_INSTALLED_APPS = [
    {
        appId: "com.psru.lostfound",
        appName: "PSRU Lost & Found",
        icon: "psru",
        version: "1.0.0",
        sizeMb: 38,
        state: "STOPPED"
    },
    {
        appId: "com.android.settings",
        appName: "การตั้งค่า",
        icon: "settings",
        version: "15.0.0",
        sizeMb: 30,
        state: "STOPPED"
    }
];

let currentPhoneState = null;
let dialBuffer = "";
let calcBuffer = "0";

// Start automatic state polling
document.addEventListener("DOMContentLoaded", () => {
    initFirebase();
    updateClock();
    setInterval(updateClock, 1000);
    fetchPhoneState();
    setInterval(fetchPhoneState, 3000);

    // Request browser push notification permission upon first interaction
    const requestNotifPermission = () => {
        if (typeof window !== "undefined" && "Notification" in window && Notification.permission === "default") {
            Notification.requestPermission().catch(() => {});
        }
    };
    document.addEventListener("click", requestNotifPermission, { once: true });
    document.addEventListener("touchstart", requestNotifPermission, { once: true });
});

function updateClock() {
    const now = new Date();
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    const timeStr = `${hh}:${mm}`;

    const statusClock = document.getElementById("statusClock");
    if (statusClock) statusClock.textContent = timeStr;

    const homeClock = document.getElementById("homeClock");
    if (homeClock) homeClock.textContent = timeStr;
}

// ============================================================================
// SUPER ADMIN IDS CONFIGURATION (รองรับหลาย Admin ทั้งระบบ)
// ============================================================================
const SUPER_ADMIN_IDS = ["6812247050", "6812247020"];

function isUserAdmin(u) {
    if (!u) return false;
    return u.role === 'ADMIN' || u.isAdmin === true || SUPER_ADMIN_IDS.includes(String(u.studentId));
}

// ============================================================================
// FIREBASE CLOUD FIRESTORE INTEGRATION
// ============================================================================
const firebaseConfig = {
  apiKey: "AIzaSyCR9qeghYUq1ozrNzFlEX4SodgH1K466PI",
  authDomain: "projectoop-28c8a.firebaseapp.com",
  projectId: "projectoop-28c8a",
  storageBucket: "projectoop-28c8a.firebasestorage.app",
  messagingSenderId: "837180086007",
  appId: "1:837180086007:web:781bc97b3dffc4ead1a5f2",
  measurementId: "G-PJHKYHCG6B"
};

let firestoreDb = null;
let isFirebaseOnline = false;

function initFirebase() {
    try {
        if (typeof firebase !== 'undefined') {
            if (!firebase.apps.length) {
                firebase.initializeApp(firebaseConfig);
            }
            firestoreDb = firebase.firestore();
            isFirebaseOnline = true;
            console.log("🔥 Firebase Cloud Firestore connected! (projectoop-28c8a)");
            showCloudIndicator();
            setupFirestoreRealtimeListener();
            seedDefaultUserToFirestore();
        }
    } catch (err) {
        console.warn("Firebase init warning:", err);
    }
}

async function seedDefaultUserToFirestore() {
    if (!firestoreDb) return;
    try {
        for (const adminId of SUPER_ADMIN_IDS) {
            const docRef = firestoreDb.collection("users").doc(adminId);
            const userDoc = await docRef.get();
            if (userDoc.exists) {
                if (userDoc.data().role !== "ADMIN") {
                    await docRef.update({ role: "ADMIN" });
                    console.log(`🔥 Updated ${adminId} to ADMIN in Firestore`);
                }
            } else if (adminId === "6812247050") {
                await docRef.set(SIM_DEFAULT_USERS[0]);
                console.log("🔥 Seeded user 6812247050 to Firestore");
            }
        }
    } catch (e) {
        console.warn("Firestore seed user check:", e);
    }
}

function setupFirestoreRealtimeListener() {
    if (!firestoreDb) return;

    // 1. REAL-TIME POSTS SYNC (Single Source of Truth)
    firestoreDb.collection("posts").onSnapshot(snapshot => {
        const cloudPosts = [];
        snapshot.forEach(doc => {
            const data = doc.data();
            if (!data.id) data.id = doc.id;
            cloudPosts.push(data);
        });

        cloudPosts.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

        // Detect newly created posts across all devices in real-time
        if (!isInitialPostsLoad) {
            cloudPosts.forEach(post => {
                if (post.id && !knownPostIds.has(post.id)) {
                    knownPostIds.add(post.id);
                    // Trigger alert if recent (within 5 minutes)
                    const isRecent = !post.createdAt || (Date.now() - post.createdAt < 5 * 60 * 1000);
                    if (isRecent) {
                        triggerNewPostNotification(post);
                    }
                }
            });
        } else {
            cloudPosts.forEach(p => {
                if (p.id) knownPostIds.add(p.id);
            });
            isInitialPostsLoad = false;
        }

        // Unconditionally sync simPosts to cloudPosts (even when empty!)
        simPosts = cloudPosts;
        saveSimPosts();

        if (clientSimState && clientSimState.runningApp && clientSimState.runningApp.appData) {
            clientSimState.runningApp.appData.posts = simPosts;
            clientSimState.runningApp.appData.items = simPosts;
        }
        if (currentPhoneState && currentPhoneState.runningApp && currentPhoneState.runningApp.appData) {
            currentPhoneState.runningApp.appData.posts = simPosts;
            currentPhoneState.runningApp.appData.items = simPosts;
        }

        lastRenderedAppKey = "";
        if (clientSimState) {
            renderPhoneUI(clientSimState);
        } else if (currentPhoneState) {
            renderPhoneUI(currentPhoneState);
        }

        if (currentDatabaseData) {
            currentDatabaseData.posts = simPosts;
            const pCount = document.getElementById("dbPostsCount");
            if (pCount) pCount.textContent = simPosts.length;
            if (currentDbTab === 'posts') renderDatabaseTabContent();
        }
    }, err => {
        console.warn("Firestore posts onSnapshot:", err);
    });

    // 2. REAL-TIME USERS SYNC
    firestoreDb.collection("users").onSnapshot(snapshot => {
        const cloudUsers = [];
        snapshot.forEach(doc => {
            const u = doc.data();
            if (!u.studentId) u.studentId = doc.id;
            cloudUsers.push(u);
        });
        if (cloudUsers.length > 0) {
            cloudUsers.forEach(u => {
                if (SUPER_ADMIN_IDS.includes(String(u.studentId))) {
                    u.role = "ADMIN";
                }
            });
            simUsers = cloudUsers;
            saveSimUsers();
            if (currentDatabaseData) {
                currentDatabaseData.users = simUsers.map(u => ({
                    studentId: u.studentId,
                    fullName: u.fullName,
                    faculty: u.faculty,
                    instagram: u.instagram,
                    line: u.line || "",
                    hasProfilePhoto: !!u.profileImage,
                    registeredAt: u.registeredAt
                }));
                const uCount = document.getElementById("dbUsersCount");
                if (uCount) uCount.textContent = simUsers.length;
                if (currentDbTab === 'users') renderDatabaseTabContent();
            }
        }
    }, err => {
        console.warn("Firestore users onSnapshot:", err);
    });

    // 3. REAL-TIME ADMIN BROADCASTS SYNC (Multi-Device Broadcast)
    firestoreDb.collection("broadcasts").orderBy("createdAt", "desc").limit(10).onSnapshot(snapshot => {
        const cloudBroadcasts = [];
        snapshot.forEach(doc => {
            const b = doc.data();
            if (!b.id) b.id = doc.id;
            cloudBroadcasts.push(b);
        });

        // Detect if there is a new broadcast newer than last seen
        if (cloudBroadcasts.length > 0) {
            const newest = cloudBroadcasts[0];
            if (newest.createdAt && newest.createdAt > lastSeenBroadcastTime) {
                showBroadcastToast(newest);
                lastSeenBroadcastTime = newest.createdAt;
                try {
                    localStorage.setItem("psru_last_bc_time", String(lastSeenBroadcastTime));
                } catch(e) {}
            }
        }

        activeBroadcasts = cloudBroadcasts;
        saveActiveBroadcasts();

        // Also ensure broadcasts appear in phone notification shade
        const targetNotifications = (clientSimState && clientSimState.notifications) || (currentPhoneState && currentPhoneState.notifications);
        if (targetNotifications) {
            cloudBroadcasts.forEach(bc => {
                const alreadyHas = targetNotifications.some(n => n.id === bc.id || (n.timestamp === bc.createdAt));
                if (!alreadyHas) {
                    targetNotifications.unshift({
                        id: bc.id,
                        title: "📢 " + (bc.title || "ประกาศจากผู้ดูแลระบบ"),
                        message: (bc.message || "") + " (" + (bc.senderName || "Admin") + ")",
                        appSource: "PSRU Broadcast",
                        timestamp: bc.createdAt || Date.now()
                    });
                }
            });
            renderNotificationShade(targetNotifications);
        }

        lastRenderedAppKey = "";
        if (clientSimState) {
            renderPhoneUI(clientSimState);
        } else if (currentPhoneState) {
            renderPhoneUI(currentPhoneState);
        }
    }, err => {
        console.warn("Firestore broadcasts onSnapshot:", err);
    });
}

// Standalone Cloud / Offline Mode Simulation Support (e.g. Vercel)
let isOfflineMode = false;
let clientSimState = null;

const SIM_DEFAULT_USERS = [
    {
        studentId: "6812247050",
        fullName: "พีรนาท สิงห์โต",
        faculty: "คณะวิศวกรรมศาสตร์และเทคโนโลยีอุตสาหกรรม",
        instagram: "@s.peeranart",
        profileImage: "uploads/profile_user_1789807298340_a924f1.jpg",
        password: "123456",
        role: "ADMIN",
        registeredAt: 1789807209333
    }
];

let simUsers = (() => {
    try {
        const stored = localStorage.getItem("psru_sim_users");
        const list = stored ? JSON.parse(stored) : SIM_DEFAULT_USERS;
        list.forEach(u => {
            if (SUPER_ADMIN_IDS.includes(String(u.studentId))) u.role = "ADMIN";
        });
        return list;
    } catch(e) {
        return SIM_DEFAULT_USERS;
    }
})();

function saveSimUsers() {
    try {
        localStorage.setItem("psru_sim_users", JSON.stringify(simUsers));
    } catch(e) {}
}

// Persistent User Session Management (Keep logged in across page reloads)
function getSavedSessionUser() {
    try {
        const stored = localStorage.getItem("psru_session_user");
        if (stored) {
            const u = JSON.parse(stored);
            if (u && u.studentId) {
                if (SUPER_ADMIN_IDS.includes(String(u.studentId))) u.role = "ADMIN";
                const matched = simUsers.find(x => x.studentId === u.studentId);
                if (matched && SUPER_ADMIN_IDS.includes(String(matched.studentId))) matched.role = "ADMIN";
                return matched || u;
            }
        }
    } catch(e) {}
    return null;
}

function saveSessionUser(user) {
    try {
        if (user && user.studentId) {
            localStorage.setItem("psru_session_user", JSON.stringify(user));
        } else {
            localStorage.removeItem("psru_session_user");
        }
    } catch(e) {}
}

function clearSessionUser() {
    try {
        localStorage.removeItem("psru_session_user");
    } catch(e) {}
}

const SIM_SAMPLE_POSTS = [
    {
        id: "ITEM-101",
        type: "LOST",
        title: "iPad Air 5 สีฟ้า (เคสสีดำ)",
        category: "อุปกรณ์อิเล็กทรอนิกส์ (มือถือ, iPad, โน้ตบุ๊ก, หูฟัง)",
        location: "ห้องสมุดกลาง ชั้น 2 (โซนอ่านหนังสือเงียบ)",
        description: "ลืมไว้ที่โต๊ะอ่านหนังสือช่วงเวลา 13:00 น. มีสติกเกอร์รูปแมวติดหลังเคส",
        photoUrl: "",
        reporterId: "6812247050",
        reporterName: "พีรนาท สิงห์โต",
        reporterFaculty: "คณะวิศวกรรมศาสตร์และเทคโนโลยีอุตสาหกรรม",
        reporterIg: "@s.peeranart",
        status: "OPEN",
        createdAt: Date.now() - 7200000
    },
    {
        id: "ITEM-102",
        type: "FOUND",
        title: "บัตรนักศึกษา และ บัตรประชาชน",
        category: "บัตร / เอกสารสำคัญ / บัตรนักศึกษา",
        location: "โรงอาหารกลาง ชั้น 1 ใกล้ร้านข้าวมันไก่",
        description: "เก็บตกอยู่บนโต๊ะอาหาร ติดต่อขอรับคืนได้ทาง IG เลยครับ",
        photoUrl: "",
        reporterId: "6812345678",
        reporterName: "สมชาย ใจดี",
        reporterFaculty: "คณะวิทยาการจัดการ",
        reporterIg: "@somchai_psru",
        status: "OPEN",
        createdAt: Date.now() - 18000000
    }
];

let simPosts = (() => {
    try {
        const stored = localStorage.getItem("psru_sim_posts");
        return stored !== null ? JSON.parse(stored) : [];
    } catch(e) {
        return [];
    }
})();

function saveSimPosts() {
    try {
        localStorage.setItem("psru_sim_posts", JSON.stringify(simPosts));
    } catch(e) {}
}

let activeBroadcasts = (() => {
    try {
        const stored = localStorage.getItem("psru_sim_broadcasts");
        return stored !== null ? JSON.parse(stored) : [];
    } catch(e) {
        return [];
    }
})();

function saveActiveBroadcasts() {
    try {
        localStorage.setItem("psru_sim_broadcasts", JSON.stringify(activeBroadcasts));
    } catch(e) {}
}

let lastSeenBroadcastTime = Number(localStorage.getItem("psru_last_bc_time") || "0");
let toastTimer = null;
let currentToastPostId = null;
let isInitialPostsLoad = true;
const knownPostIds = new Set();

function playPostNotificationSound(type) {
    try {
        const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        if (audioCtx.state === 'suspended') {
            audioCtx.resume();
        }
        const now = audioCtx.currentTime;
        const playTone = (freq, startOffset, duration) => {
            const osc = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            osc.type = "sine";
            osc.frequency.setValueAtTime(freq, now + startOffset);
            gain.gain.setValueAtTime(0.2, now + startOffset);
            gain.gain.exponentialRampToValueAtTime(0.001, now + startOffset + duration);
            osc.connect(gain);
            gain.connect(audioCtx.destination);
            osc.start(now + startOffset);
            osc.stop(now + startOffset + duration);
        };
        if (type === 'LOST') {
            playTone(659.25, 0.00, 0.15); // E5
            playTone(880.00, 0.12, 0.25); // A5
        } else {
            playTone(523.25, 0.00, 0.12); // C5
            playTone(659.25, 0.10, 0.12); // E5
            playTone(783.99, 0.20, 0.25); // G5
        }
    } catch (e) {}
}

function sendBrowserNotification(title, body, icon) {
    try {
        if (typeof window !== "undefined" && "Notification" in window) {
            if (Notification.permission === "granted") {
                new Notification(title, {
                    body: body,
                    icon: icon || "psru_icon.jpg"
                });
            } else if (Notification.permission === "default") {
                Notification.requestPermission().then(perm => {
                    if (perm === "granted") {
                        new Notification(title, {
                            body: body,
                            icon: icon || "psru_icon.jpg"
                        });
                    }
                }).catch(() => {});
            }
        }
    } catch (e) {}
}

function showBroadcastToast(bc) {
    if (!bc) return;
    const toast = document.getElementById("broadcastToastAlert");
    if (!toast) return;

    currentToastPostId = null;
    toast.classList.remove('toast-theme-lost', 'toast-theme-found');

    const tBadge = document.getElementById("toastBadge");
    const tTitle = document.getElementById("toastTitle");
    const tMsg = document.getElementById("toastMsg");
    const tSender = document.getElementById("toastSender");

    if (tBadge) tBadge.textContent = "🚨 ประกาศด่วน (Broadcast)";
    if (tTitle) tTitle.textContent = bc.title || "ประกาศด่วนจากระบบ";
    if (tMsg) tMsg.textContent = bc.message || "";
    if (tSender) tSender.textContent = "โดย: " + (bc.senderName || "Super Admin");

    toast.style.display = "block";
    playPostNotificationSound('LOST');

    sendBrowserNotification(
        "🚨 ประกาศด่วน: " + (bc.title || ''),
        bc.message || '',
        "psru_icon.jpg"
    );

    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        dismissBroadcastToast();
    }, 7000);
}

function showPostNotificationToast(post) {
    if (!post) return;
    const toast = document.getElementById("broadcastToastAlert");
    if (!toast) return;

    currentToastPostId = post.id;
    const isLost = post.type === 'LOST';

    toast.classList.remove('toast-theme-lost', 'toast-theme-found');
    toast.classList.add(isLost ? 'toast-theme-lost' : 'toast-theme-found');

    const tBadge = document.getElementById("toastBadge");
    const tTitle = document.getElementById("toastTitle");
    const tMsg = document.getElementById("toastMsg");
    const tSender = document.getElementById("toastSender");

    if (tBadge) {
        tBadge.textContent = isLost 
            ? '📢 มีคนโพสต์ใหม่: 🔴 ตามหาของ' 
            : '📢 มีคนโพสต์ใหม่: 🟢 แจ้งพบของ';
    }
    if (tTitle) {
        tTitle.textContent = (isLost ? '🔴 ตามหา: ' : '🟢 พบของ: ') + (post.title || 'ไม่ระบุชื่อสิ่งของ');
    }
    if (tMsg) {
        const loc = post.location ? `📍 ${post.location}` : '';
        const desc = post.description ? ` (${post.description})` : '';
        tMsg.textContent = `${loc}${desc} • คลิกเพื่อเปิดดูประกาศ`;
    }
    if (tSender) {
        tSender.textContent = `👤 โดยคุณ: ${post.reporterName || 'สมาชิก'} (${post.reporterFaculty || 'มรภ.พิบูลสงคราม'})`;
    }

    toast.style.display = "block";
    playPostNotificationSound(post.type);

    sendBrowserNotification(
        (isLost ? '🔴 มีคนโพสต์ตามหาของ: ' : '🟢 มีคนแจ้งพบของ: ') + (post.title || ''),
        `สถานที่: ${post.location || '-'} โดย ${post.reporterName || 'สมาชิก'}`,
        post.photoUrl || "psru_icon.jpg"
    );

    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        dismissBroadcastToast();
    }, 8000);
}

function handleToastClick(event) {
    if (event && event.target && event.target.classList.contains('toast-close-btn')) {
        dismissBroadcastToast();
        return;
    }

    if (currentToastPostId) {
        const targetId = currentToastPostId;
        dismissBroadcastToast();

        // Launch app and switch to feed tab
        launchApp("com.psru.lostfound");
        setPsruUserTab("FEED");

        setTimeout(() => {
            const cardEl = document.querySelector(`[data-post-id="${targetId}"]`) || document.querySelector('.feed-card');
            if (cardEl) {
                cardEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                cardEl.classList.add('feed-card-highlight');
                setTimeout(() => cardEl.classList.remove('feed-card-highlight'), 3000);
            }
        }, 350);
    } else {
        dismissBroadcastToast();
    }
}

function dismissBroadcastToast() {
    const toast = document.getElementById("broadcastToastAlert");
    if (toast) toast.style.display = "none";
    if (toastTimer) {
        clearTimeout(toastTimer);
        toastTimer = null;
    }
    currentToastPostId = null;
}

function triggerNewPostNotification(post) {
    if (!post) return;

    // 1. Show floating push toast on screen & play sound & browser notification
    showPostNotificationToast(post);

    // 2. Add to phone notification shade & status bar badge (Observer Pattern)
    const typeLabel = (post.type === 'LOST') ? '🔴 ตามหาของหาย' : '🟢 แจ้งพบของ';
    const notifItem = {
        id: 'post_notif_' + (post.id || Date.now()),
        title: `📢 ${typeLabel}: ${post.title || 'ของหาย/พบของ'}`,
        message: `${post.location || 'ภายใน มรภ.พิบูลสงคราม'} (โดย: ${post.reporterName || 'ผู้ใช้'})`,
        appSource: 'PSRU Lost & Found',
        timestamp: post.createdAt || Date.now(),
        postId: post.id
    };

    const targetNotifications = (clientSimState && clientSimState.notifications) || (currentPhoneState && currentPhoneState.notifications);
    if (targetNotifications) {
        const alreadyHas = targetNotifications.some(n => n.id === notifItem.id || (n.postId && n.postId === post.id));
        if (!alreadyHas) {
            targetNotifications.unshift(notifItem);
            renderNotificationShade(targetNotifications);
            const nBadge = document.getElementById("notifCount");
            if (nBadge) nBadge.textContent = targetNotifications.length;
        }
    }
}

function showCloudIndicator() {
    const controls = document.querySelector(".floating-controls");
    if (!controls) return;
    let badge = document.getElementById("cloudSimBadge");
    if (!badge) {
        badge = document.createElement("div");
        badge.id = "cloudSimBadge";
        badge.className = "cloud-sim-badge";
        controls.prepend(badge);
    }
    badge.innerHTML = "🔥 Firebase Cloud Connected";
    badge.title = "เชื่อมต่อฐานข้อมูล Google Cloud Firestore สำเร็จ (Project: projectoop-28c8a)";
}

function hideCloudIndicator() {
    const badge = document.getElementById("cloudSimBadge");
    if (badge) badge.remove();
}

function enableClientSimulation() {
    isOfflineMode = true;
    const savedUser = getSavedSessionUser();

    clientSimState = {
        device: {
            brand: "PSRU Android Virtual",
            model: "CPE Mobile Simulator",
            serialNumber: "SN-6812247050",
            deviceType: "SMARTPHONE",
            isPoweredOn: true,
            volume: 80,
            simState: "READY",
            uptimeSeconds: 3600
        },
        battery: {
            level: 100,
            capacityMah: 5000,
            isCharging: true,
            temperatureCelsius: 26,
            isCritical: false,
            isInfinite: true
        },
        display: {
            resolution: "1080x2400 FHD+",
            diagonalInches: 6.7,
            brightness: 85,
            isScreenOn: true,
            refreshRateHz: 120
        },
        storage: {
            totalMb: 128000,
            usedMb: 14500,
            freeMb: 113500,
            usedPercent: 11,
            files: [
                { fileName: "system.img", sizeMb: 8500, fileType: "SYSTEM", createdAt: Date.now() - 86400000 },
                { fileName: "profile_6812247050.jpg", sizeMb: 2, fileType: "image/jpeg", createdAt: Date.now() - 3600000 },
                { fileName: "users.dat", sizeMb: 1, fileType: "DATA", createdAt: Date.now() - 7200000 }
            ]
        },
        network: {
            connectionType: "Wi-Fi 6 (PSRU-Campus-5G)",
            signalStrength: 98,
            isOnline: true
        },
        notifications: [
            {
                title: "ยินดีต้อนรับสู่ PSRU Lost & Found",
                message: "ระบบจำลองสมาร์ตโฟนเชื่อมต่อพร้อมใช้งานบน Cloud",
                appSource: "PSRU Portal",
                timestamp: Date.now()
            }
        ],
        installedApps: DEFAULT_INSTALLED_APPS,
        runningApp: {
            appId: "com.psru.lostfound",
            appName: "PSRU Lost & Found",
            state: "RESUMED",
            appData: {
                isLoggedIn: !!savedUser,
                currentUser: savedUser,
                items: simPosts,
                posts: simPosts
            }
        }
    };

    currentPhoneState = clientSimState;
    renderPhoneUI(clientSimState);
    renderInspector(clientSimState);
    showCloudIndicator();
}

function executeSimAction(action, params) {
    if (!clientSimState) {
        enableClientSimulation();
    }
    if (!clientSimState.runningApp) {
        const savedUser = getSavedSessionUser();
        clientSimState.runningApp = {
            appId: "com.psru.lostfound",
            appName: "PSRU Lost & Found",
            state: "RESUMED",
            appData: { isLoggedIn: !!savedUser, currentUser: savedUser, items: simPosts, posts: simPosts }
        };
    }
    const appData = clientSimState.runningApp.appData;

    switch (action) {
        case "LOGIN": {
            const sid = String(params.studentId || "").trim();
            const pwd = String(params.password || "").trim();
            const found = simUsers.find(u => u.studentId === sid && u.password === pwd);
            if (!found) {
                return { success: false, message: "รหัสนักศึกษาหรือรหัสผ่านไม่ถูกต้อง" };
            }
            if (SUPER_ADMIN_IDS.includes(String(found.studentId))) found.role = "ADMIN";
            appData.isLoggedIn = true;
            appData.currentUser = found;
            saveSessionUser(found);
            return { success: true, message: "เข้าสู่ระบบสำเร็จ", data: { user: found } };
        }
        case "RESTORE_SESSION": {
            const sid = String(params.studentId || "").trim();
            const found = simUsers.find(u => u.studentId === sid);
            if (found) {
                if (SUPER_ADMIN_IDS.includes(String(found.studentId))) found.role = "ADMIN";
                appData.isLoggedIn = true;
                appData.currentUser = found;
                saveSessionUser(found);
                return { success: true, message: "คืนค่าเซสชันสำเร็จ", data: { user: found } };
            }
            return { success: false, message: "ไม่พบผู้ใช้" };
        }
        case "REGISTER": {
            const sid = String(params.studentId || "").trim();
            const exists = simUsers.some(u => u.studentId === sid);
            if (exists) {
                return { success: false, message: "รหัสนักศึกษานี้ลงทะเบียนไว้แล้ว" };
            }
            const newUser = {
                studentId: sid,
                fullName: params.fullName || "",
                faculty: params.faculty || "",
                instagram: params.instagram || "",
                line: params.line || "",
                profileImage: params.profileImage || "",
                password: params.password || "",
                role: SUPER_ADMIN_IDS.includes(String(sid)) ? "ADMIN" : "STUDENT",
                registeredAt: Date.now()
            };
            simUsers.push(newUser);
            saveSimUsers();
            if (firestoreDb) {
                firestoreDb.collection("users").doc(newUser.studentId).set(newUser).catch(e => console.warn("Firestore user sync:", e));
            }
            appData.isLoggedIn = true;
            appData.currentUser = newUser;
            saveSessionUser(newUser);
            return { success: true, message: "ลงทะเบียนสำเร็จ", data: { user: newUser } };
        }
        case "LOGOUT": {
            appData.isLoggedIn = false;
            appData.currentUser = null;
            clearSessionUser();
            return { success: true, message: "ออกจากระบบแล้ว" };
        }
        case "CREATE_POST": {
            const user = appData.currentUser || simUsers[0];
            const newPost = {
                id: "ITEM-" + Date.now().toString().slice(-4),
                type: params.type || "LOST",
                title: params.title || "",
                category: params.category || "อุปกรณ์อิเล็กทรอนิกส์ (มือถือ, iPad, โน้ตบุ๊ก, หูฟัง)",
                location: params.location || "",
                description: params.description || "",
                photoUrl: params.photoUrl || "",
                reporterId: user.studentId,
                reporterName: user.fullName,
                reporterFaculty: user.faculty,
                reporterIg: user.instagram,
                reporterLine: user.line || "",
                reporterPhoto: user.profileImage || "",
                status: "OPEN",
                createdAt: Date.now()
            };
            simPosts.unshift(newPost);
            saveSimPosts();
            if (firestoreDb) {
                firestoreDb.collection("posts").doc(newPost.id).set(newPost).catch(e => console.warn("Firestore post sync:", e));
            }
            appData.posts = simPosts;
            appData.items = simPosts;
            knownPostIds.add(newPost.id);
            triggerNewPostNotification(newPost);
            return { success: true, message: "บันทึกประกาศสำเร็จ", data: { post: newPost } };
        }
        case "RESOLVE_POST": {
            const post = simPosts.find(p => p.id === params.itemId);
            if (post) {
                post.status = "RESOLVED";
                saveSimPosts();
                if (firestoreDb) {
                    firestoreDb.collection("posts").doc(params.itemId).update({ status: "RESOLVED" }).catch(e => console.warn("Firestore resolve sync:", e));
                }
                appData.posts = simPosts;
                appData.items = simPosts;
                return { success: true, message: "ปรับสถานะเรียบร้อยแล้ว" };
            }
            return { success: false, message: "ไม่พบประกาศที่ระบุ" };
        }
        case "DELETE_POST": {
            const targetId = params.itemId;
            simPosts = simPosts.filter(p => p.id !== targetId);
            saveSimPosts();
            if (firestoreDb && targetId) {
                firestoreDb.collection("posts").doc(targetId).delete().catch(e => console.warn("Firestore delete sync:", e));
                firestoreDb.collection("posts").where("id", "==", targetId).get().then(snap => {
                    snap.forEach(doc => doc.ref.delete());
                }).catch(e => console.warn("Firestore query delete:", e));
            }
            appData.posts = simPosts;
            appData.items = simPosts;
            return { success: true, message: "ลบประกาศเรียบร้อยแล้ว" };
        }
        case "ADMIN_DELETE_USER": {
            if (SUPER_ADMIN_IDS.includes(String(params.studentId))) {
                return { success: false, message: "ไม่สามารถลบบัญชี Super Admin ได้" };
            }
            simUsers = simUsers.filter(u => u.studentId !== params.studentId);
            saveSimUsers();
            if (firestoreDb) {
                firestoreDb.collection("users").doc(params.studentId).delete().catch(e => console.warn(e));
            }
            return { success: true, message: "ลบบัญชีนักศึกษารหัส " + params.studentId + " เรียบร้อยแล้ว" };
        }
        case "ADMIN_RESET_PASSWORD": {
            const target = simUsers.find(u => u.studentId === params.studentId);
            if (target) {
                target.password = params.newPassword || "123456";
                saveSimUsers();
                if (firestoreDb) {
                    firestoreDb.collection("users").doc(params.studentId).update({ password: target.password }).catch(e => console.warn(e));
                }
                return { success: true, message: "รีเซ็ตรหัสผ่านสำหรับรหัส " + params.studentId + " เป็น " + target.password + " สำเร็จ" };
            }
            return { success: false, message: "ไม่พบบัญชีผู้ใช้" };
        }
        case "ADMIN_BROADCAST": {
            const title = params.title || "ประกาศจากผู้ดูแลระบบ";
            const message = params.message || "";
            if (!message.trim()) return { success: false, message: "กรุณากรอกข้อความ" };
            
            const adminName = (appData.currentUser && appData.currentUser.fullName) || "Super Admin (พีรนาท)";
            const adminId = (appData.currentUser && appData.currentUser.studentId) || "6812247050";
            const broadcastObj = {
                id: "BC-" + Date.now(),
                title: title,
                message: message,
                senderName: adminName,
                senderId: adminId,
                createdAt: Date.now()
            };
            
            activeBroadcasts.unshift(broadcastObj);
            saveActiveBroadcasts();
            
            if (firestoreDb) {
                firestoreDb.collection("broadcasts").doc(broadcastObj.id).set(broadcastObj).catch(e => console.warn("Firestore broadcast sync:", e));
            }
            
            const notif = {
                id: broadcastObj.id,
                title: "📢 " + title,
                message: message + " (" + adminName + ")",
                appSource: "PSRU Admin",
                timestamp: Date.now()
            };
            clientSimState.notifications.unshift(notif);
            showBroadcastToast(broadcastObj);
            return { success: true, message: "ส่งประกาศด่วนขึ้นระบบสำเร็จ", data: { broadcast: broadcastObj } };
        }
        case "ADMIN_DELETE_BROADCAST": {
            const bcId = params.broadcastId;
            activeBroadcasts = activeBroadcasts.filter(b => b.id !== bcId);
            saveActiveBroadcasts();
            if (firestoreDb && bcId) {
                firestoreDb.collection("broadcasts").doc(bcId).delete().catch(e => console.warn(e));
                firestoreDb.collection("broadcasts").where("id", "==", bcId).get().then(snap => snap.forEach(d => d.ref.delete())).catch(e => {});
            }
            return { success: true, message: "ลบประกาศด่วนเรียบร้อยแล้ว" };
        }
        case "ADMIN_PIN_POST": {
            const idx = simPosts.findIndex(p => p.id === params.itemId);
            if (idx !== -1) {
                const item = simPosts.splice(idx, 1)[0];
                simPosts.unshift(item);
                saveSimPosts();
                appData.posts = simPosts;
                appData.items = simPosts;
                return { success: true, message: "ปักหมุดประกาศขึ้นบนสุดสำเร็จ" };
            }
            return { success: false, message: "ไม่พบประกาศ" };
        }
        case "ADMIN_CLEAR_RESOLVED": {
            const resolvedPosts = simPosts.filter(p => p.status === 'RESOLVED');
            const before = simPosts.length;
            simPosts = simPosts.filter(p => p.status !== 'RESOLVED');
            const cleared = before - simPosts.length;
            saveSimPosts();
            if (firestoreDb) {
                resolvedPosts.forEach(p => {
                    firestoreDb.collection("posts").doc(p.id).delete().catch(e => console.warn(e));
                    firestoreDb.collection("posts").where("id", "==", p.id).get().then(snap => snap.forEach(d => d.ref.delete())).catch(e => {});
                });
            }
            appData.posts = simPosts;
            appData.items = simPosts;
            return { success: true, message: "ล้างประกาศที่ส่งคืนแล้วจำนวน " + cleared + " รายการ" };
        }
        case "UPDATE_PROFILE": {
            if (appData.currentUser) {
                if (params.instagram !== undefined) appData.currentUser.instagram = params.instagram;
                if (params.profileImage !== undefined) appData.currentUser.profileImage = params.profileImage;
                saveSimUsers();
                saveSessionUser(appData.currentUser);
                if (firestoreDb) {
                    firestoreDb.collection("users").doc(appData.currentUser.studentId).update({
                        instagram: appData.currentUser.instagram,
                        profileImage: appData.currentUser.profileImage
                    }).catch(e => console.warn("Firestore profile sync:", e));
                }
            }
            return { success: true, message: "อัปเดตโปรไฟล์เรียบร้อยแล้ว" };
        }
        case "TOGGLE_WIFI": {
            if (!appData.wifiEnabled) appData.wifiEnabled = false;
            appData.wifiEnabled = !appData.wifiEnabled;
            return { success: true };
        }
        case "TOGGLE_CELLULAR": {
            if (!appData.cellularEnabled) appData.cellularEnabled = false;
            appData.cellularEnabled = !appData.cellularEnabled;
            return { success: true };
        }
        case "SET_BRIGHTNESS": {
            clientSimState.display.brightness = Number(params.brightness) || 75;
            return { success: true };
        }
        case "SET_VOLUME": {
            clientSimState.device.volume = Number(params.volume) || 70;
            return { success: true };
        }
        default:
            return { success: true };
    }
}

// Fetch Full Phone State from Java Localhost Backend (with seamless cloud fallback)
async function fetchPhoneState() {
    const savedUser = getSavedSessionUser();
    try {
        const res = await fetch("/api/state");
        if (!res.ok) throw new Error("Backend response " + res.status);
        const state = await res.json();
        isOfflineMode = false;
        hideCloudIndicator();

        // If backend has user logged in, preserve it in localStorage
        if (state.runningApp && state.runningApp.appData) {
            if (state.runningApp.appData.isLoggedIn && state.runningApp.appData.currentUser) {
                saveSessionUser(state.runningApp.appData.currentUser);
            } else if (!state.runningApp.appData.isLoggedIn && savedUser) {
                // Restore session on server
                sendAppAction("RESTORE_SESSION", { studentId: savedUser.studentId });
                state.runningApp.appData.isLoggedIn = true;
                state.runningApp.appData.currentUser = savedUser;
            }

            // Offline / Localhost new post detection
            const items = state.runningApp.appData.items || state.runningApp.appData.posts || [];
            if (!firestoreDb || !isFirebaseOnline) {
                if (!isInitialPostsLoad) {
                    items.forEach(p => {
                        if (p.id && !knownPostIds.has(p.id)) {
                            knownPostIds.add(p.id);
                            const isRecent = !p.createdAt || (Date.now() - p.createdAt < 5 * 60 * 1000);
                            if (isRecent) {
                                triggerNewPostNotification(p);
                            }
                        }
                    });
                } else {
                    items.forEach(p => { if (p.id) knownPostIds.add(p.id); });
                    isInitialPostsLoad = false;
                }
            }
        }

        currentPhoneState = state;
        renderPhoneUI(state);
        renderInspector(state);
    } catch (err) {
        if (!isOfflineMode || !clientSimState) {
            enableClientSimulation();
        } else {
            if (clientSimState.runningApp && clientSimState.runningApp.appData) {
                if (!clientSimState.runningApp.appData.isLoggedIn && savedUser) {
                    clientSimState.runningApp.appData.isLoggedIn = true;
                    clientSimState.runningApp.appData.currentUser = savedUser;
                }
            }
            renderPhoneUI(clientSimState);
            renderInspector(clientSimState);
        }
    }
}

// Render Virtual Phone UI
function renderPhoneUI(state) {
    if (!state) return;

    // 1. Power & Screen Status
    const screenOffOverlay = document.getElementById("screenOffOverlay");
    const isPowered = state.device && state.device.isPoweredOn;
    const isScreenOn = state.display && state.display.isScreenOn;

    if (!isPowered || !isScreenOn) {
        screenOffOverlay.classList.add("active");
    } else {
        screenOffOverlay.classList.remove("active");
    }

    // 2. Status Bar
    const isInfinite = state.battery && state.battery.isInfinite;
    const batteryLvl = state.battery ? state.battery.level : 100;
    document.getElementById("batteryPercent").textContent = isInfinite ? "100% ∞" : (batteryLvl + "%");
    document.getElementById("batteryIcon").textContent = "⚡🔋";

    const wifiIcon = document.getElementById("wifiIcon");
    if (state.network && state.network.wifiEnabled) {
        wifiIcon.textContent = "📶";
        wifiIcon.title = state.network.connectionType;
    } else if (state.network && state.network.cellularEnabled) {
        wifiIcon.textContent = "📡 5G";
    } else {
        wifiIcon.textContent = "✈️";
    }

    // 3. Notifications Badge
    const notifs = state.notifications || [];
    document.getElementById("notifCount").textContent = notifs.length;
    renderNotificationShade(notifs);

    // 4. Apps Grid (Home View)
    renderAppsGrid(state.installedApps || []);

    // 5. Active App or Home Screen View
    const homeView = document.getElementById("homeView");
    const appView = document.getElementById("appView");

    if (state.runningApp) {
        homeView.style.display = "none";
        appView.style.display = "flex";
        renderActiveApp(state.runningApp);
    } else {
        homeView.style.display = "flex";
        appView.style.display = "none";
        lastRenderedAppKey = "";
    }
}

function getCombinedApps(apps) {
    const list = [...(apps || [])];
    const existingIds = new Set(list.map(a => a.appId));
    DEFAULT_INSTALLED_APPS.forEach(defApp => {
        if (!existingIds.has(defApp.appId)) {
            list.push(defApp);
        }
    });
    return list;
}

let lastRenderedAppsJson = "";

// Render Installed Apps in Launcher
function renderAppsGrid(apps) {
    const effectiveApps = getCombinedApps(apps);
    const appsJson = JSON.stringify(effectiveApps.map(a => a.appId + a.appName));
    if (appsJson === lastRenderedAppsJson) return;
    lastRenderedAppsJson = appsJson;

    const grid = document.getElementById("appsGrid");
    if (!grid) return;
    grid.innerHTML = "";

    const iconClasses = {
        "phone": "icon-dialer",
        "camera": "icon-camera",
        "note": "icon-notes",
        "calc": "icon-calculator",
        "settings": "icon-settings"
    };

    const emojis = {
        "phone": "📞",
        "camera": "📷",
        "note": "📝",
        "calc": "🧮",
        "settings": "⚙️"
    };

    effectiveApps.forEach(app => {
        const item = document.createElement("div");
        item.className = "app-icon-item";
        item.onclick = () => launchApp(app.appId);

        const circle = document.createElement("div");
        if (app.icon === "psru") {
            circle.className = "app-icon-circle icon-psru-wrapper";
            circle.innerHTML = `<img src="psru_icon.jpg" onerror="this.onerror=null; this.src='web/psru_icon.jpg'; this.onerror=function(){this.parentElement.innerHTML='🎒';};" alt="PSRU Lost&Found" class="app-icon-img" />`;
        } else {
            circle.className = `app-icon-circle ${iconClasses[app.icon] || 'icon-settings'}`;
            circle.textContent = emojis[app.icon] || "📱";
        }

        const label = document.createElement("div");
        label.className = "app-label";
        label.textContent = app.appName;

        item.appendChild(circle);
        item.appendChild(label);
        grid.appendChild(item);
    });
}

let lastRenderedAppKey = "";

// State preservation for forms
const formDraft = {
    regStudentId: "",
    regFullName: "",
    regFaculty: "",
    regInstagram: "",
    regPhoto: "",
    regPassword: "",
    loginStudentId: "",
    loginPassword: ""
};

function saveDraftValue(id, value) {
    formDraft[id] = value;
}

// State preservation for New Post
const postDraft = {
    type: 'LOST', // 'LOST' or 'FOUND'
    title: '',
    category: 'อุปกรณ์อิเล็กทรอนิกส์',
    location: '',
    description: '',
    photo: ''
};

function savePostDraft(field, val) {
    postDraft[field] = val;
}

function setPostDraftType(type) {
    postDraft.type = type;
    lastRenderedAppKey = "";
    if (currentPhoneState && currentPhoneState.runningApp) {
        renderActiveApp(currentPhoneState.runningApp);
    }
}

let psruAuthTab = 'LOGIN'; // 'LOGIN' or 'REGISTER' (for unauthenticated view)
let psruUserTab = 'FEED';  // 'FEED', 'NEW_POST', 'PROFILE' (for logged-in view)
let feedFilter = 'ALL';    // 'ALL', 'LOST', 'FOUND'

function setPsruAuthTab(tab) {
    if (psruAuthTab === tab) return;
    psruAuthTab = tab;
    lastRenderedAppKey = "";
    if (currentPhoneState && currentPhoneState.runningApp) {
        renderActiveApp(currentPhoneState.runningApp);
    } else {
        fetchPhoneState();
    }
}

function setPsruUserTab(tab) {
    if (psruUserTab === tab) return;
    psruUserTab = tab;
    lastRenderedAppKey = "";
    if (currentPhoneState && currentPhoneState.runningApp) {
        renderActiveApp(currentPhoneState.runningApp);
    } else {
        fetchPhoneState();
    }
}

function setFeedFilter(filter) {
    if (feedFilter === filter) return;
    feedFilter = filter;
    lastRenderedAppKey = "";
    if (currentPhoneState && currentPhoneState.runningApp) {
        renderActiveApp(currentPhoneState.runningApp);
    } else {
        fetchPhoneState();
    }
}

// Render Active App Screen
function renderActiveApp(app) {
    const titleEl = document.getElementById("appHeaderTitle");
    const statusEl = document.getElementById("appHeaderStatus");
    const bodyEl = document.getElementById("appContentBody");

    if (titleEl) titleEl.textContent = app.appName;
    if (statusEl) statusEl.textContent = app.state;

    const appId = app.appId;
    const data = app.appData || {};

    const appStateKey = `${appId}:${app.state}:${psruAuthTab}:${psruUserTab}:${feedFilter}:${JSON.stringify(data)}`;
    const isUserTyping = bodyEl && document.activeElement && bodyEl.contains(document.activeElement);

    // If app state hasn't changed at all, DO NOT touch innerHTML (prevents wiping user typing/focus)
    if (appStateKey === lastRenderedAppKey) {
        return;
    }

    // If user is actively typing in the form, do not interrupt them unless the app itself changed
    if (isUserTyping && lastRenderedAppKey.startsWith(appId + ":" + app.state + ":" + psruAuthTab + ":" + psruUserTab)) {
        return;
    }

    lastRenderedAppKey = appStateKey;

    if (appId === "com.android.settings") {
        bodyEl.innerHTML = renderSettingsAppHtml(data);
    } else if (appId === "com.psru.lostfound") {
        bodyEl.innerHTML = renderPsruAppHtml(data);
    } else if (appId === "com.android.dialer") {
        bodyEl.innerHTML = renderDialerAppHtml();
    } else if (appId === "com.android.camera") {
        bodyEl.innerHTML = renderCameraAppHtml();
    } else if (appId === "com.android.calc") {
        bodyEl.innerHTML = renderCalcAppHtml();
    } else if (appId === "com.android.notes") {
        bodyEl.innerHTML = renderNotesAppHtml();
    } else {
        bodyEl.innerHTML = `<div style="padding:20px; text-align:center;">App running: ${app.appName}</div>`;
    }
}

// Main PSRU App HTML Renderer
function renderPsruAppHtml(data) {
    if (data.isLoggedIn && data.currentUser) {
        const user = data.currentUser;
        const items = (data.items && data.items.length > 0) ? data.items : ((data.posts && data.posts.length > 0) ? data.posts : (simPosts || []));

        return `
            <div class="portal-card portal-card-logged-in">
                <!-- App Top Bar -->
                <div class="portal-header logged-in-header">
                    <div class="portal-header-left">
                        <div class="portal-title neon-green">PSRU LOST &amp; FOUND</div>
                        <div class="portal-subtitle">CPE PORTAL &bull; ของหายได้คืน มรภ.พิบูลสงคราม</div>
                    </div>
                    <button class="portal-close-btn" onclick="pressHome()">✕</button>
                </div>

                <!-- Sub Navigation Bar (ฟีดประกาศ | ลงประกาศ | จัดการระบบ | โปรไฟล์) -->
                <div class="portal-subnav-bar">
                    <button class="portal-subnav-btn ${psruUserTab === 'FEED' ? 'active' : ''}" onclick="setPsruUserTab('FEED')">
                        <span class="subnav-icon">📋</span>
                        <span class="subnav-text">ฟีด (${items.length})</span>
                    </button>
                    <button class="portal-subnav-btn ${psruUserTab === 'NEW_POST' ? 'active' : ''}" onclick="setPsruUserTab('NEW_POST')">
                        <span class="subnav-icon">➕</span>
                        <span class="subnav-text">ลงประกาศ</span>
                    </button>
                    ${isUserAdmin(user) ? `
                    <button class="portal-subnav-btn ${psruUserTab === 'ADMIN' ? 'active' : ''}" onclick="setPsruUserTab('ADMIN')">
                        <span class="subnav-icon">👑</span>
                        <span class="subnav-text">จัดการ</span>
                    </button>
                    ` : ''}
                    <button class="portal-subnav-btn ${psruUserTab === 'PROFILE' ? 'active' : ''}" onclick="setPsruUserTab('PROFILE')">
                        <span class="subnav-icon">👤</span>
                        <span class="subnav-text">โปรไฟล์</span>
                    </button>
                </div>

                <!-- Tab Content Body -->
                <div class="portal-tab-content">
                    ${psruUserTab === 'FEED' ? renderPsruFeedTab(data) : (psruUserTab === 'NEW_POST' ? renderPsruNewPostTab(data) : (psruUserTab === 'ADMIN' ? renderPsruAdminTab(data) : renderPsruProfileTab(data)))}
                </div>

                <!-- App Footer -->
                <div class="portal-footer">
                    Powered By ArPo Breez
                </div>
            </div>
        `;
    }

    // Unauthenticated: Login / Register Form
    return `
        <div class="portal-card">
            <div class="portal-header">
                <div class="portal-title neon-green">CPE PORTAL</div>
                <div class="portal-subtitle">Create and Develop By ARPO CPE</div>
                <button class="portal-close-btn" onclick="pressHome()">✕</button>
            </div>

            <!-- Tab Switcher (เข้าสู่ระบบ / สมัครสมาชิก) -->
            <div class="portal-tabs-pill">
                <button class="portal-tab-btn ${psruAuthTab === 'LOGIN' ? 'active' : ''}" onclick="setPsruAuthTab('LOGIN')">
                    เข้าสู่ระบบ
                </button>
                <button class="portal-tab-btn ${psruAuthTab === 'REGISTER' ? 'active' : ''}" onclick="setPsruAuthTab('REGISTER')">
                    สมัครสมาชิก
                </button>
            </div>

            <!-- Form Content -->
            ${psruAuthTab === 'LOGIN' ? renderPsruLoginForm() : renderPsruRegisterForm()}

            <!-- Bottom Link -->
            <div class="portal-bottom-link">
                ${psruAuthTab === 'LOGIN' ? `
                    ยังไม่มีบัญชี? <a href="javascript:void(0)" onclick="setPsruAuthTab('REGISTER')">สมัครสมาชิกที่นี่</a>
                ` : `
                    มีบัญชีอยู่แล้ว? <a href="javascript:void(0)" onclick="setPsruAuthTab('LOGIN')">เข้าสู่ระบบที่นี่</a>
                `}
            </div>

            <!-- Footer Text -->
            <div class="portal-footer">
                Powered By ArPo Breez
            </div>
        </div>
    `;
}

// 1. FEED TAB: Render Lost & Found Announcements
function renderPsruFeedTab(data) {
    const rawItems = (data.items && data.items.length > 0) ? data.items : ((data.posts && data.posts.length > 0) ? data.posts : (simPosts || []));
    const currentUser = data.currentUser || {};

    let filteredItems = rawItems;
    if (feedFilter === 'LOST') {
        filteredItems = rawItems.filter(item => item.type === 'LOST');
    } else if (feedFilter === 'FOUND') {
        filteredItems = rawItems.filter(item => item.type === 'FOUND');
    }

    const lostCount = rawItems.filter(i => i.type === 'LOST').length;
    const foundCount = rawItems.filter(i => i.type === 'FOUND').length;

    let itemsHtml = '';
    if (filteredItems.length === 0) {
        itemsHtml = `
            <div class="feed-empty-state">
                <div class="empty-icon">🔍</div>
                <div class="empty-title">ยังไม่มีประกาศในหมวดนี้</div>
                <div class="empty-sub">คุณสามารถเป็นคนแรกที่ลงประกาศตามหาหรือแจ้งพบของได้ทันที</div>
                <button class="btn-portal-submit mt-2" onclick="setPsruUserTab('NEW_POST')">
                    ➕ ลงประกาศสิ่งของชิ้นแรก
                </button>
            </div>
        `;
    } else {
        itemsHtml = filteredItems.map(item => {
            const isLost = item.type === 'LOST';
            const isOwner = currentUser.studentId === item.reporterId;
            const isResolved = item.status === 'RESOLVED';
            const igHandle = item.reporterIg || '@arpo_cpe';
            const reporterUser = (typeof simUsers !== 'undefined' && Array.isArray(simUsers)) 
                ? simUsers.find(u => u.studentId === item.reporterId) 
                : null;
            const reporterPhoto = (reporterUser && reporterUser.profileImage) || item.reporterPhoto || '';
            const reporterInitial = (item.reporterName && item.reporterName.trim().length > 0) 
                ? item.reporterName.trim().charAt(0) 
                : '👤';
            const cleanPhotoUrl = (reporterPhoto && reporterPhoto.trim().length > 0 && reporterPhoto !== 'data:image/jpeg;base64') ? reporterPhoto : '';

            return `
                <div class="feed-card ${isLost ? 'card-lost' : 'card-found'}" data-post-id="${escapeHtml(item.id || '')}">
                    <!-- Card Top Header -->
                    <div class="feed-card-header">
                        <div class="feed-badge-group">
                            <span class="feed-type-badge ${isLost ? 'type-lost' : 'type-found'}">
                                ${isLost ? '🔴 ตามหาของหาย' : '🟢 แจ้งพบของ'}
                            </span>
                            ${isResolved ? '<span class="feed-status-badge status-resolved">✅ ส่งคืนแล้ว</span>' : '<span class="feed-status-badge status-open">⏳ กำลังค้นหา</span>'}
                        </div>
                        <div class="feed-time-text">${formatPostTime(item.createdAt)}</div>
                    </div>

                    <!-- Item Title -->
                    <div class="feed-item-title">${escapeHtml(item.title)}</div>

                    <!-- Meta: Category & Location -->
                    <div class="feed-meta-row">
                        <span class="feed-meta-pill">🏷️ ${escapeHtml(item.category || 'ทั่วไป')}</span>
                        <span class="feed-meta-pill">📍 ${escapeHtml(item.location || 'ภายในมหาวิทยาลัย')}</span>
                    </div>

                    <!-- Optional Item Photo -->
                    ${(item.photoUrl && item.photoUrl.trim().length > 0 && item.photoUrl !== 'data:image/jpeg;base64') ? `
                        <div class="feed-photo-container">
                            <img src="${item.photoUrl}" class="feed-photo-img" alt="รูปสิ่งของ" onerror="this.parentElement.style.display='none';" />
                        </div>
                    ` : ''}

                    <!-- Item Description -->
                    ${item.description ? `
                        <div class="feed-item-desc">${escapeHtml(item.description)}</div>
                    ` : ''}

                    <!-- Reporter Info with Profile Avatar & Modal View -->
                    <div class="feed-reporter-row" onclick="handleViewReporterProfile('${escapeHtml(item.reporterId || '')}', '${escapeHtml(item.reporterName || '')}', '${escapeHtml(item.reporterFaculty || '')}', '${escapeHtml(igHandle)}', '${escapeHtml(item.id || '')}')" title="คลิกเพื่อดูรูปโปรไฟล์และข้อมูลผู้ลงประกาศ">
                        <div class="feed-reporter-left">
                            <div class="feed-reporter-avatar-thumb">
                                ${cleanPhotoUrl ? `
                                    <img src="${cleanPhotoUrl}" class="reporter-avatar-img" alt="${escapeHtml(item.reporterName)}" onerror="this.style.display='none'; this.nextElementSibling.style.display='block';" />
                                    <span class="avatar-fallback-initial" style="display:none;">${escapeHtml(reporterInitial)}</span>
                                ` : `
                                    <span class="avatar-fallback-initial">${escapeHtml(reporterInitial)}</span>
                                `}
                                <span class="avatar-zoom-icon">🔍</span>
                            </div>
                            <div class="feed-reporter-info">
                                <span class="reporter-name">👤 ${escapeHtml(item.reporterName)}</span>
                                <span class="reporter-faculty">${escapeHtml(item.reporterFaculty)}</span>
                            </div>
                        </div>
                        <div class="feed-view-profile-hint">
                            <span>ดูโปรไฟล์</span> 👁️
                        </div>
                    </div>

                    <!-- Instagram Contact Button -->
                    <div class="feed-contact-wrapper">
                        <div class="feed-ig-btn" onclick="handleOpenInstagram('${escapeHtml(igHandle)}')" title="คลิกเพื่อดู Instagram">
                            <div class="feed-ig-icon">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect>
                                    <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path>
                                    <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line>
                                </svg>
                            </div>
                            <div class="feed-ig-text">
                                <span class="feed-ig-label">ติดต่อผ่าน IG:</span>
                                <span class="feed-ig-handle">${escapeHtml(igHandle)}</span>
                            </div>
                            <span class="feed-ig-arrow">↗</span>
                        </div>
                    </div>

                    <!-- Manage Actions for Post Owner or Admin -->
                    ${(isOwner || isUserAdmin(currentUser)) ? `
                        <div class="feed-owner-actions">
                            ${!isResolved ? `
                                <button class="btn-owner-action btn-resolve" onclick="handleResolvePost('${item.id}')">
                                    ✅ ปรับเป็นส่งคืนแล้ว${!isOwner ? ' (Admin)' : ''}
                                </button>
                            ` : ''}
                            <button class="btn-owner-action btn-delete" onclick="handleDeletePost('${item.id}')">
                                🗑️ ลบประกาศ${!isOwner ? ' (Admin)' : ''}
                            </button>
                            ${isUserAdmin(currentUser) ? `
                                <button class="btn-owner-action" style="background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.4);" onclick="handleAdminPinPost('${item.id}')" title="ปักหมุดขึ้นบนสุด">
                                    📌 ปักหมุด
                                </button>
                            ` : ''}
                        </div>
                    ` : ''}
                </div>
            `;
        }).join('');
    }

    return `
        <!-- Urgent Admin Broadcast Banner (Multi-Device Realtime) -->
        ${(activeBroadcasts && activeBroadcasts.length > 0) ? `
            <div class="urgent-broadcast-banner">
                <div class="urgent-broadcast-top">
                    <span class="urgent-badge">🚨 ประกาศด่วน</span>
                    <span class="urgent-time">${formatPostTime(activeBroadcasts[0].createdAt)}</span>
                </div>
                <div class="urgent-broadcast-title">📢 ${escapeHtml(activeBroadcasts[0].title || 'ประกาศด่วนจากผู้ดูแลระบบ')}</div>
                <div class="urgent-broadcast-msg">${escapeHtml(activeBroadcasts[0].message || '')}</div>
                <div class="urgent-broadcast-footer">
                    <span class="urgent-broadcast-author">โดย: ${escapeHtml(activeBroadcasts[0].senderName || 'Super Admin')}</span>
                    ${isUserAdmin(currentUser) ? `
                        <button class="btn-text" style="color:#f87171; font-size:0.65rem;" onclick="handleAdminDeleteBroadcast('${escapeHtml(activeBroadcasts[0].id)}')">
                            🗑️ ลบประกาศนี้
                        </button>
                    ` : ''}
                </div>
            </div>
        ` : ''}

        <!-- Filter Bar -->
        <div class="feed-filter-bar">
            <button class="feed-filter-btn ${feedFilter === 'ALL' ? 'active' : ''}" onclick="setFeedFilter('ALL')">
                ทั้งหมด (${rawItems.length})
            </button>
            <button class="feed-filter-btn ${feedFilter === 'LOST' ? 'active' : ''}" onclick="setFeedFilter('LOST')">
                🔴 ของหาย (${lostCount})
            </button>
            <button class="feed-filter-btn ${feedFilter === 'FOUND' ? 'active' : ''}" onclick="setFeedFilter('FOUND')">
                🟢 แจ้งพบ (${foundCount})
            </button>
        </div>

        <!-- Quick Post Shortcut Banner -->
        <div class="feed-action-banner" onclick="setPsruUserTab('NEW_POST')">
            <div class="feed-action-banner-text">
                <div class="banner-title">📢 ประกาศของหาย หรือ แจ้งพบของ?</div>
                <div class="banner-sub">คลิกเพื่อลงประกาศให้เพื่อนๆ ช่วยตามหา</div>
            </div>
            <button class="btn-banner-post">➕ โพสต์</button>
        </div>

        <!-- Feed List -->
        <div class="feed-items-list">
            ${itemsHtml}
        </div>
    `;
}

// 2. NEW POST TAB: Create Lost & Found Announcement Form
const PSRU_CATEGORIES = [
    "อุปกรณ์อิเล็กทรอนิกส์ (มือถือ, iPad, โน้ตบุ๊ก, หูฟัง)",
    "บัตร / เอกสารสำคัญ / บัตรนักศึกษา",
    "กระเป๋า / กระเป๋าสตางค์",
    "กุญแจ / พวงกุญแจหอพัก",
    "เครื่องแต่งกาย / เสื้อผ้า / รองเท้า",
    "แว่นตา / นาฬิกา / เครื่องประดับ",
    "ของใช้ส่วนตัวอื่นๆ"
];

function renderPsruNewPostTab(data) {
    const user = data.currentUser || {};
    const categoryOptions = PSRU_CATEGORIES.map(cat => {
        const isSel = postDraft.category === cat ? "selected" : "";
        return `<option value="${escapeHtml(cat)}" ${isSel}>${escapeHtml(cat)}</option>`;
    }).join("");

    return `
        <div class="portal-new-post-form">
            <div class="post-form-heading">
                <div class="post-form-title">📝 ลงประกาศของหาย / แจ้งพบของ</div>
                <div class="post-form-sub">กรอกข้อมูลเพื่อให้ผู้พบเห็นสามารถติดต่อส่งคืนได้รวดเร็ว</div>
            </div>

            <!-- Post Type Segmented Control -->
            <div class="post-type-selector">
                <button type="button" class="btn-post-type ${postDraft.type === 'LOST' ? 'active-lost' : ''}" onclick="setPostDraftType('LOST')">
                    🔴 ตามหาของหาย
                    <span class="type-hint">ฉันทำสิ่งของหาย</span>
                </button>
                <button type="button" class="btn-post-type ${postDraft.type === 'FOUND' ? 'active-found' : ''}" onclick="setPostDraftType('FOUND')">
                    🟢 แจ้งพบของที่หาย
                    <span class="type-hint">ฉันเก็บสิ่งของได้</span>
                </button>
            </div>

            <!-- Input: Item Title -->
            <div class="portal-input-group">
                <label>ชื่อสิ่งของที่หายหรือพบ *</label>
                <input type="text" id="postTitle" placeholder="เช่น iPad Air สีฟ้า, กระเป๋าสตางค์สีน้ำตาล" 
                    value="${escapeHtml(postDraft.title || '')}"
                    oninput="savePostDraft('title', this.value)" />
            </div>

            <!-- Input: Category -->
            <div class="portal-input-group">
                <label>หมวดหมู่สิ่งของ</label>
                <select id="postCategory" onchange="savePostDraft('category', this.value)">
                    ${categoryOptions}
                </select>
            </div>

            <!-- Input: Location -->
            <div class="portal-input-group">
                <label>สถานที่พบ หรือ จุดที่คาดว่าทำหาย *</label>
                <input type="text" id="postLocation" placeholder="เช่น โรงอาหารกลาง ชั้น 1, หน้าตึกวิศวะ" 
                    value="${escapeHtml(postDraft.location || '')}"
                    oninput="savePostDraft('location', this.value)" />
            </div>

            <!-- Input: Description -->
            <div class="portal-input-group">
                <label>รายละเอียดเพิ่มเติม</label>
                <textarea id="postDescription" rows="3" placeholder="ระบุลักษณะโดยละเอียด เช่น สี, รอยตำหนิ, ยี่ห้อ, วันและเวลาที่คาดว่าหาย..." 
                    oninput="savePostDraft('description', this.value)">${escapeHtml(postDraft.description || '')}</textarea>
            </div>

            <!-- Input: Photo Upload -->
            <div class="portal-input-group">
                <label>รูปภาพสิ่งของ (ถ้ามี)</label>
                <div class="portal-photo-picker-row">
                    <div class="portal-photo-thumbnail" id="postPhotoThumbnail">
                        ${postDraft.photo ? `<img src="${postDraft.photo}" class="reg-photo-img" />` : `<span>📷</span>`}
                    </div>
                    <div style="flex:1;">
                        <button type="button" class="btn-portal-upload" onclick="document.getElementById('postPhotoPicker').click()">
                            📁 เลือกรูปภาพสิ่งของ
                        </button>
                        <input type="file" id="postPhotoPicker" accept="image/*" style="display:none;" onchange="handlePostPhotoUpload(event)" />
                        <div class="photo-upload-hint">แนบรูปถ่ายเพื่อให้สังเกตได้ง่ายขึ้น</div>
                    </div>
                </div>
            </div>

            <!-- Contact Preview (Attached Instagram) -->
            <div class="post-ig-preview-box">
                <div class="preview-label">ช่องทางติดต่อที่จะแสดงในประกาศ:</div>
                <div class="preview-content">
                    <div class="preview-ig-icon">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect>
                            <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path>
                            <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line>
                        </svg>
                    </div>
                    <div class="preview-ig-info">
                        <span class="preview-ig-handle">${escapeHtml(user.instagram || user.phone || '@arpo_cpe')}</span>
                        <span class="preview-ig-source">(ดึงข้อมูลจากโปรไฟล์ของคุณ)</span>
                    </div>
                </div>
            </div>

            <!-- Submit & Cancel Buttons -->
            <button type="button" class="btn-portal-submit" onclick="handleCreatePost()">
                📢 โพสต์ประกาศทันที
            </button>
            <button type="button" class="btn-portal-cancel" onclick="setPsruUserTab('FEED')">
                ✕ ยกเลิก (กลับไปที่ฟีด)
            </button>
        </div>
    `;
}

// 3. PROFILE TAB: Render Profile Management & Logout
function renderPsruProfileTab(data) {
    const user = data.currentUser || {};
    const hasPhoto = user.profileImage && user.profileImage.trim().length > 0 && user.profileImage !== 'data:image/jpeg;base64';
    const avatarHtml = hasPhoto
        ? `<img src="${user.profileImage}" class="portal-avatar-img" alt="Profile" onerror="this.onerror=null; this.parentElement.innerHTML='<div class=\'portal-avatar-fallback\'><span>👨‍🎓</span></div>';" />`
        : `<div class="portal-avatar-fallback"><span>👨‍🎓</span></div>`;

    return `
        <div class="portal-profile-box">
            <!-- Profile Photo with Camera Upload Badge -->
            <div class="portal-avatar-wrapper" onclick="document.getElementById('profilePhotoPicker').click()" title="คลิกเพื่อเลือก/เปลี่ยนรูปตัวเอง">
                ${avatarHtml}
                <div class="portal-avatar-edit-badge" title="เปลี่ยนรูปโปรไฟล์">📷</div>
            </div>
            <input type="file" id="profilePhotoPicker" accept="image/*" style="display:none;" onchange="handleProfilePhotoChange(event)" />
            <div class="portal-change-photo-btn" onclick="document.getElementById('profilePhotoPicker').click()">
                📷 เปลี่ยนรูปโปรไฟล์ตัวเอง
            </div>

            <div class="portal-user-name">${escapeHtml(user.fullName)}</div>
            <div class="portal-user-id">รหัสนักศึกษา: ${escapeHtml(user.studentId)}</div>
            ${isUserAdmin(user) ? `
                <div class="admin-badge-gold" style="margin: 4px auto 6px auto;">👑 ผู้ดูแลระบบสูงสุด (SUPER ADMIN)</div>
            ` : ''}
            <div class="portal-user-faculty">🏛️ ${escapeHtml(user.faculty || 'คณะวิศวกรรมศาสตร์และเทคโนโลยีอุตสาหกรรม')}</div>

            <!-- Contact: ONLY Instagram (IG) -->
            <div class="portal-ig-contact-card" onclick="handleEditInstagramPrompt('${escapeHtml(user.instagram || user.phone || '')}')" title="คลิกเพื่อแก้ไข IG">
                <div class="portal-ig-icon">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect>
                        <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path>
                        <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line>
                    </svg>
                </div>
                <div class="portal-ig-content">
                    <div class="portal-ig-label">Contact (Instagram)</div>
                    <div class="portal-ig-handle">${escapeHtml(user.instagram || user.phone || '@arpo_cpe')}</div>
                </div>
                <div class="portal-ig-edit" title="แก้ไข IG">✏️</div>
            </div>

            <div class="portal-badge-active">● เข้าสู่ระบบแล้ว (Active Session)</div>

            <button class="btn-portal-submit mt-4" onclick="handlePsruLogout()">
                LOGOUT (ออกจากระบบ)
            </button>
        </div>
    `;
}


// Compress and optimize image to ensure clean data and fast upload
function compressImageFile(file, maxWidth, maxHeight, quality, callback) {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
            const canvas = document.createElement('canvas');
            let width = img.width;
            let height = img.height;

            if (width > maxWidth || height > maxHeight) {
                if (width / height > maxWidth / maxHeight) {
                    height = Math.round((height * maxWidth) / width);
                    width = maxWidth;
                } else {
                    width = Math.round((width * maxHeight) / height);
                    height = maxHeight;
                }
            }

            canvas.width = width;
            canvas.height = height;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0, width, height);
            const dataUrl = canvas.toDataURL('image/jpeg', quality || 0.85);
            callback(dataUrl);
        };
        img.onerror = () => {
            callback(e.target.result);
        };
        img.src = e.target.result;
    };
    reader.readAsDataURL(file);
}

// Handling Post Actions
function handlePostPhotoUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    compressImageFile(file, 800, 800, 0.85, (dataUrl) => {
        postDraft.photo = dataUrl;
        const thumb = document.getElementById("postPhotoThumbnail");
        if (thumb) {
            thumb.innerHTML = `<img src="${dataUrl}" class="reg-photo-img" />`;
        }
    });
}

async function handleCreatePost() {
    const title = (document.getElementById("postTitle")?.value || postDraft.title || "").trim();
    const location = (document.getElementById("postLocation")?.value || postDraft.location || "").trim();
    const category = document.getElementById("postCategory")?.value || postDraft.category || "อุปกรณ์อิเล็กทรอนิกส์";
    const description = (document.getElementById("postDescription")?.value || postDraft.description || "").trim();
    const photoUrl = postDraft.photo || "";

    if (!title) {
        alert("กรุณาระบุชื่อสิ่งของที่หายหรือพบ");
        return;
    }
    if (!location) {
        alert("กรุณาระบุสถานที่พบหรือจุดที่ทำหาย");
        return;
    }

    pulsePillar('pillarPolymorphism');
    pulsePillar('pillarEncapsulation');

    const res = await sendAppAction("CREATE_POST", {
        type: postDraft.type,
        title: title,
        category: category,
        location: location,
        description: description,
        photoUrl: photoUrl
    });

    if (res.success) {
        // Reset postDraft
        postDraft.title = '';
        postDraft.location = '';
        postDraft.description = '';
        postDraft.photo = '';
        postDraft.category = 'อุปกรณ์อิเล็กทรอนิกส์';

        // If newly created post was returned, ensure it is unshifted into simPosts
        const createdItem = (res.data && res.data.post) ? res.data.post : (res.data || null);
        if (createdItem && createdItem.id) {
            if (!simPosts.some(p => p.id === createdItem.id)) {
                simPosts.unshift(createdItem);
                saveSimPosts();
            }
        }

        // Switch to Feed tab to view the newly created post immediately
        psruUserTab = 'FEED';
        lastRenderedAppKey = "";
        
        if (clientSimState && clientSimState.runningApp && clientSimState.runningApp.appData) {
            clientSimState.runningApp.appData.items = simPosts;
            clientSimState.runningApp.appData.posts = simPosts;
            renderPhoneUI(clientSimState);
        }

        alert("🎉 โพสต์ประกาศสำเร็จเรียบร้อยแล้ว!");
    } else {
        alert(res.message || "เกิดข้อผิดพลาดในการโพสต์ประกาศ");
    }
    fetchPhoneState();
}

async function handleDeletePost(itemId) {
    if (!confirm("คุณต้องการลบประกาศนี้ใช่หรือไม่?")) {
        return;
    }
    pulsePillar('pillarEncapsulation');

    // 1. Instant optimistic local deletion
    simPosts = simPosts.filter(p => p.id !== itemId);
    saveSimPosts();
    if (clientSimState && clientSimState.runningApp && clientSimState.runningApp.appData) {
        clientSimState.runningApp.appData.items = simPosts;
        clientSimState.runningApp.appData.posts = simPosts;
    }
    if (currentPhoneState && currentPhoneState.runningApp && currentPhoneState.runningApp.appData) {
        currentPhoneState.runningApp.appData.items = simPosts;
        currentPhoneState.runningApp.appData.posts = simPosts;
    }
    lastRenderedAppKey = "";
    if (clientSimState) {
        renderPhoneUI(clientSimState);
    } else if (currentPhoneState) {
        renderPhoneUI(currentPhoneState);
    }

    // 2. Direct Firestore deletion (doc id and query match)
    if (firestoreDb && itemId) {
        firestoreDb.collection("posts").doc(itemId).delete().catch(e => console.warn(e));
        firestoreDb.collection("posts").where("id", "==", itemId).get().then(s => s.forEach(d => d.ref.delete())).catch(e => {});
    }

    // 3. Dispatch to backend if available
    const res = await sendAppAction("DELETE_POST", { itemId });
    if (!res.success && res.message && !res.message.includes("สำเร็จ")) {
        console.warn("Backend delete info:", res.message);
    }
    lastRenderedAppKey = "";
    fetchPhoneState();
}

async function handleResolvePost(itemId) {
    if (!confirm("ทำเครื่องหมายว่าพบ/ส่งคืนเจ้าของแล้ว?")) {
        return;
    }
    pulsePillar('pillarEncapsulation');
    const p = simPosts.find(x => x.id === itemId);
    if (p) p.status = "RESOLVED";
    saveSimPosts();
    if (firestoreDb && itemId) {
        firestoreDb.collection("posts").doc(itemId).update({ status: "RESOLVED" }).catch(e => {});
    }
    lastRenderedAppKey = "";
    if (clientSimState) renderPhoneUI(clientSimState);
    const res = await sendAppAction("RESOLVE_POST", { itemId });
    if (!res.success) {
        alert(res.message || "ไม่สามารถปรับสถานะได้");
    } else {
        lastRenderedAppKey = "";
    }
    fetchPhoneState();
}

function handleOpenInstagram(igHandle) {
    pulsePillar('pillarPolymorphism');
    const cleanHandle = String(igHandle || '').replace('@', '').trim();
    if (cleanHandle) {
        window.open(`https://instagram.com/${cleanHandle}`, '_blank');
    }
}

function handleViewReporterProfile(reporterId, fallbackName, fallbackFaculty, fallbackIg, postId) {
    let user = null;
    if (typeof simUsers !== 'undefined' && Array.isArray(simUsers) && reporterId) {
        user = simUsers.find(u => u.studentId === reporterId);
    }
    if (!user && clientSimState && clientSimState.runningApp && clientSimState.runningApp.appData) {
        const cu = clientSimState.runningApp.appData.currentUser;
        if (cu && cu.studentId === reporterId) {
            user = cu;
        }
    }

    let post = null;
    if (postId && typeof simPosts !== 'undefined' && Array.isArray(simPosts)) {
        post = simPosts.find(p => p.id === postId);
    }

    const name = (user && user.fullName) || fallbackName || 'ไม่ระบุชื่อ';
    const studentId = reporterId || (user && user.studentId) || '-';
    const faculty = (user && user.faculty) || fallbackFaculty || 'มหาวิทยาลัยราชภัฏพิบูลสงคราม';
    const ig = (user && user.instagram) || fallbackIg || '';
    const rawPhoto = (user && user.profileImage) || (post && post.reporterPhoto) || '';
    const photo = (rawPhoto && rawPhoto.trim().length > 0 && rawPhoto !== 'data:image/jpeg;base64') ? rawPhoto.trim() : '';

    const modal = document.getElementById('reporterProfileModal');
    if (!modal) return;

    const avatarEl = document.getElementById('modalReporterAvatar');
    const nameEl = document.getElementById('modalReporterName');
    const idEl = document.getElementById('modalReporterId');
    const facultyEl = document.getElementById('modalReporterFaculty');
    const contactRowEl = document.getElementById('modalReporterContactRow');

    if (nameEl) nameEl.textContent = name;
    if (idEl) idEl.textContent = `รหัสนักศึกษา: ${studentId}`;
    if (facultyEl) facultyEl.textContent = `🏛️ คณะ: ${faculty}`;

    if (avatarEl) {
        if (photo) {
            avatarEl.innerHTML = `<img src="${photo}" alt="${escapeHtml(name)}" style="width:100%; height:100%; object-fit:cover; border-radius:50%;" onerror="this.parentElement.innerHTML='<span style=\\'font-size:3.5rem;\\'>👤</span>';" />`;
        } else {
            const initial = name ? name.trim().charAt(0) : '👤';
            avatarEl.innerHTML = `<div style="font-size: 3.5rem; font-weight: 700; color: #38bdf8;">${escapeHtml(initial)}</div>`;
        }
    }

    if (contactRowEl) {
        if (ig && ig.trim().length > 0) {
            contactRowEl.innerHTML = `
                <a href="javascript:void(0)" onclick="handleOpenInstagram('${escapeHtml(ig)}')" class="btn-profile-ig" title="เปิด Instagram">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="2" y="2" width="20" height="20" rx="5" ry="5"></rect>
                        <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"></path>
                        <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"></line>
                    </svg>
                    <span>ติดต่อ Instagram (${escapeHtml(ig)})</span>
                </a>
            `;
        } else {
            contactRowEl.innerHTML = `
                <div style="font-size: 0.78rem; color: #94a3b8; font-style: italic;">
                    (ไม่ได้ระบุบัญชี Instagram)
                </div>
            `;
        }
    }

    modal.style.display = 'flex';
}

function closeReporterProfileModal(event) {
    if (event && event.target && event.target !== event.currentTarget &&
        !event.target.classList.contains('profile-modal-close-btn') &&
        !event.target.classList.contains('btn-profile-modal-dismiss')) {
        return;
    }
    const modal = document.getElementById('reporterProfileModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

// Close profile modal on ESC key
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const modal = document.getElementById('reporterProfileModal');
        if (modal && modal.style.display !== 'none') {
            modal.style.display = 'none';
        }
    }
});

function formatPostTime(ts) {
    if (!ts) return 'เมื่อสักครู่';
    const d = new Date(ts);
    const now = new Date();
    const diffSec = Math.floor((now - d) / 1000);
    if (diffSec < 60) return 'เมื่อสักครู่';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin} นาทีที่แล้ว`;
    const diffHr = Math.floor(diffMin / 60);
    if (diffHr < 24) return `${diffHr} ชม. ที่แล้ว`;
    return d.toLocaleDateString('th-TH', { month: 'short', day: 'numeric' });
}

function handleRegPhotoUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    compressImageFile(file, 600, 600, 0.85, (dataUrl) => {
        formDraft.regPhoto = dataUrl;
        const thumb = document.getElementById("regPhotoThumbnail");
        if (thumb) {
            thumb.innerHTML = `<img src="${dataUrl}" class="reg-photo-img" />`;
        }
    });
}

function handleProfilePhotoChange(event) {
    const file = event.target.files[0];
    if (!file) return;
    compressImageFile(file, 600, 600, 0.85, async (dataUrl) => {
        await sendAppAction("UPDATE_PROFILE", { profileImage: dataUrl });
        lastRenderedAppKey = "";
        fetchPhoneState();
    });
}

async function handleEditInstagramPrompt(currentIg) {
    const newIg = prompt("กรุณาระบุ Instagram (IG) ใหม่ของคุณ:", currentIg || "@");
    if (newIg !== null && newIg.trim() !== "") {
        await sendAppAction("UPDATE_PROFILE", { instagram: newIg.trim() });
        lastRenderedAppKey = "";
        fetchPhoneState();
    }
}

function renderPsruLoginForm() {
    return `
        <div class="portal-form-body">
            <div class="portal-input-group">
                <label>รหัสนักศึกษา (10 หลัก)</label>
                <input type="text" id="loginStudentId" placeholder="6812345678" maxlength="10" 
                    value="${escapeHtml(formDraft.loginStudentId || '')}"
                    oninput="saveDraftValue('loginStudentId', this.value)" />
            </div>

            <div class="portal-input-group">
                <label>รหัสผ่าน</label>
                <input type="password" id="loginPassword" placeholder="••••••••" 
                    value="${escapeHtml(formDraft.loginPassword || '')}"
                    oninput="saveDraftValue('loginPassword', this.value)" />
            </div>

            <button class="btn-portal-submit" onclick="handlePsruLogin()">
                LOGIN (เข้าสู่ระบบ)
            </button>

            <div style="margin-top: 14px; text-align: center; font-size: 0.76rem; color: #94a3b8;">
                ยังไม่มีบัญชีผู้ใช้งานใช่หรือไม่? 
                <a href="javascript:void(0)" onclick="setPsruAuthTab('REGISTER')" style="color: #38bdf8; font-weight: 600; text-decoration: underline;">สมัครสมาชิกที่นี่</a>
            </div>
        </div>
    `;
}

const PSRU_FACULTIES = [
    "คณะวิทยาศาสตร์และเทคโนโลยี",
    "คณะเทคโนโลยีการเกษตรและอาหาร",
    "คณะวิศวกรรมศาสตร์และเทคโนโลยีอุตสาหกรรม",
    "คณะมนุษยศาสตร์และสังคมศาสตร์",
    "คณะครุศาสตร์",
    "คณะวิทยาการจัดการ",
    "คณะพยาบาลศาสตร์",
    "คณะสังคมศาสตร์และการพัฒนาท้องถิ่น"
];

function renderPsruRegisterForm() {
    const facultyOptions = PSRU_FACULTIES.map(f => {
        const isSelected = formDraft.regFaculty === f ? "selected" : "";
        return `<option value="${escapeHtml(f)}" ${isSelected}>${escapeHtml(f)}</option>`;
    }).join("");

    return `
        <div class="portal-form-body">
            <div class="portal-input-group">
                <label>รหัสนักศึกษา (10 หลัก)</label>
                <input type="text" id="regStudentId" placeholder="6812345678" maxlength="10" 
                    value="${escapeHtml(formDraft.regStudentId || '')}"
                    oninput="saveDraftValue('regStudentId', this.value)" />
            </div>

            <div class="portal-input-group">
                <label>ชื่อ - นามสกุล</label>
                <input type="text" id="regFullName" placeholder="เช่น พิชญางกูร ทอฝันบางนา" 
                    value="${escapeHtml(formDraft.regFullName || '')}"
                    oninput="saveDraftValue('regFullName', this.value)" />
            </div>

            <div class="portal-input-group">
                <label>เลือกคณะ</label>
                <select id="regFaculty" onchange="saveDraftValue('regFaculty', this.value)">
                    <option value="" disabled ${!formDraft.regFaculty ? "selected" : ""}>-- กรุณาเลือกคณะ --</option>
                    ${facultyOptions}
                </select>
            </div>

            <!-- Contact: ONLY Instagram (IG) -->
            <div class="portal-input-group">
                <label>Contact: Instagram (IG)</label>
                <input type="text" id="regInstagram" placeholder="@your_instagram" 
                    value="${escapeHtml(formDraft.regInstagram || '')}"
                    oninput="saveDraftValue('regInstagram', this.value)" />
            </div>

        

            <!-- Profile Picture Upload -->
            <div class="portal-input-group">
                <label>รูปภาพโปรไฟล์ตัวเอง (รูปถ่ายนักศึกษา)</label>
                <div class="portal-photo-picker-row">
                    <div class="portal-photo-thumbnail" id="regPhotoThumbnail">
                        ${formDraft.regPhoto ? `<img src="${formDraft.regPhoto}" class="reg-photo-img" />` : `<span>📷</span>`}
                    </div>
                    <div style="flex:1;">
                        <button type="button" class="btn-portal-upload" onclick="document.getElementById('regPhotoPicker').click()">
                            📁 เลือกรูปภาพตัวเอง
                        </button>
                        <input type="file" id="regPhotoPicker" accept="image/*" style="display:none;" onchange="handleRegPhotoUpload(event)" />
                        <div class="photo-upload-hint">คลิกเพื่อเลือกไฟล์รูปภาพจากเครื่อง</div>
                    </div>
                </div>
            </div>

            <div class="portal-input-group">
                <label>รหัสผ่าน</label>
                <input type="password" id="regPassword" placeholder="กำหนดรหัสผ่าน..." 
                    value="${escapeHtml(formDraft.regPassword || '')}"
                    oninput="saveDraftValue('regPassword', this.value)" />
            </div>

            <button class="btn-portal-submit" onclick="handlePsruRegister()">
                REGISTER (ยืนยันการสมัครสมาชิก)
            </button>

            <div style="margin-top: 14px; text-align: center; font-size: 0.76rem; color: #94a3b8;">
                มีบัญชีอยู่แล้วใช่หรือไม่? 
                <a href="javascript:void(0)" onclick="setPsruAuthTab('LOGIN')" style="color: #38bdf8; font-weight: 600; text-decoration: underline;">เข้าสู่ระบบที่นี่</a>
            </div>
        </div>
    `;
}

async function handlePsruLogin() {
    const studentId = (document.getElementById("loginStudentId")?.value || formDraft.loginStudentId || "").trim();
    const password = document.getElementById("loginPassword")?.value || formDraft.loginPassword || "";
    if (!studentId || !password) {
        alert("กรุณากรอกรหัสนักศึกษาและรหัสผ่าน");
        return;
    }
    pulsePillar('pillarPolymorphism');
    pulsePillar('pillarEncapsulation');

    let res = await sendAppAction("LOGIN", { studentId, password });

    // If local/backend check fails, check directly in Firestore if available
    if (!res.success && firestoreDb) {
        try {
            const doc = await firestoreDb.collection("users").doc(studentId).get();
            if (doc.exists) {
                const u = doc.data();
                if (u.password === password) {
                    if (!simUsers.some(x => x.studentId === u.studentId)) {
                        simUsers.push(u);
                        saveSimUsers();
                    }
                    res = await sendAppAction("LOGIN", { studentId, password });
                }
            }
        } catch(e) {
            console.warn("Firestore login fallback check:", e);
        }
    }

    if (!res.success) {
        alert(res.message || "รหัสนักศึกษาหรือรหัสผ่านไม่ถูกต้อง (หากยังไม่มีบัญชี ให้กดแท็บ สมัครสมาชิก)");
    } else {
        // Land directly on FEED screen upon login
        psruUserTab = 'FEED';
        lastRenderedAppKey = "";
        alert("🎉 เข้าสู่ระบบสำเร็จ ยินดีต้อนรับ!");
    }
    fetchPhoneState();
}

async function handlePsruRegister() {
    const studentId = (document.getElementById("regStudentId")?.value || formDraft.regStudentId || "").trim();
    const fullName = (document.getElementById("regFullName")?.value || formDraft.regFullName || "").trim();
    const faculty = document.getElementById("regFaculty")?.value || formDraft.regFaculty || "";
    const instagram = (document.getElementById("regInstagram")?.value || formDraft.regInstagram || "").trim();
    const line = (document.getElementById("regLine")?.value || formDraft.regLine || "").trim();
    const profileImage = formDraft.regPhoto || "";
    const password = document.getElementById("regPassword")?.value || formDraft.regPassword || "";

    if (!studentId || studentId.length < 10) {
        alert("กรุณากรอกรหัสนักศึกษา 10 หลัก");
        return;
    }
    if (!fullName) {
        alert("กรุณากรอกชื่อ - นามสกุล");
        return;
    }
    if (!faculty) {
        alert("กรุณาเลือกคณะ");
        return;
    }
    if (!instagram) {
        alert("กรุณากรอก Instagram (IG) สำหรับติดต่อ");
        return;
    }
    if (!password) {
        alert("กรุณากรอกรหัสผ่าน");
        return;
    }

    pulsePillar('pillarPolymorphism');
    pulsePillar('pillarEncapsulation');

    const res = await sendAppAction("REGISTER", { studentId, fullName, faculty, instagram, line, profileImage, password });
    if (!res.success) {
        alert(res.message || "เกิดข้อผิดพลาดในการสมัครสมาชิก");
    } else {
        // Clear draft on successful registration and land on Feed
        formDraft.regStudentId = "";
        formDraft.regFullName = "";
        formDraft.regFaculty = "";
        formDraft.regInstagram = "";
        formDraft.regLine = "";
        formDraft.regPhoto = "";
        formDraft.regPassword = "";
        psruUserTab = 'FEED';
        lastRenderedAppKey = "";
    }
    fetchPhoneState();
}

async function handlePsruLogout() {
    pulsePillar('pillarPolymorphism');
    clearSessionUser();
    if (clientSimState && clientSimState.runningApp && clientSimState.runningApp.appData) {
        clientSimState.runningApp.appData.isLoggedIn = false;
        clientSimState.runningApp.appData.currentUser = null;
    }
    if (currentPhoneState && currentPhoneState.runningApp && currentPhoneState.runningApp.appData) {
        currentPhoneState.runningApp.appData.isLoggedIn = false;
        currentPhoneState.runningApp.appData.currentUser = null;
    }
    psruUserTab = 'FEED';
    psruAuthTab = 'LOGIN';
    lastRenderedAppKey = "";
    await sendAppAction("LOGOUT", {});
    fetchPhoneState();
}

// HTML Generator for Settings App
function renderSettingsAppHtml(data) {
    return `
        <div style="display:flex; flex-direction:column; gap:10px;">
            <div class="settings-group">
                <div class="settings-item">
                    <span>Wi-Fi Network</span>
                    <button class="btn ${data.wifiEnabled ? 'btn-primary' : 'btn-secondary'}" onclick="toggleSettingWifi()">
                        ${data.wifiEnabled ? 'เปิดอยู่ (On)' : 'ปิด (Off)'}
                    </button>
                </div>
                <div class="settings-item">
                    <span>Cellular (5G)</span>
                    <button class="btn ${data.cellularEnabled ? 'btn-primary' : 'btn-secondary'}" onclick="toggleSettingCellular()">
                        ${data.cellularEnabled ? 'เปิดอยู่ (On)' : 'ปิด (Off)'}
                    </button>
                </div>
            </div>

            <div class="settings-group">
                <div class="settings-item">
                    <span>ความสว่างหน้าจอ (Display Brightness)</span>
                    <input type="range" min="10" max="100" value="${data.brightness || 75}" onchange="setBrightness(this.value)" />
                </div>
                <div class="settings-item">
                    <span>ระดับเสียงลำโพง (Volume)</span>
                    <input type="range" min="0" max="100" value="${data.volume || 70}" onchange="setVolume(this.value)" />
                </div>
            </div>

            <div class="settings-group">
                <div style="font-weight:600; font-size:0.8rem; margin-bottom:6px; color:var(--accent-blue);">ข้อมูลเครื่อง & ระบบปฏิบัติการ (System Info)</div>
                <div class="settings-item"><span>รุ่นอุปกรณ์ (Model)</span> <span>${data.brand} ${data.model}</span></div>
                <div class="settings-item"><span>ระบบปฏิบัติการ (OS)</span> <span>${data.androidVersion} (API ${data.apiLevel})</span></div>
                <div class="settings-item"><span>Kernel</span> <span style="font-size:0.65rem;">${data.kernel}</span></div>
                <div class="settings-item"><span>Serial No.</span> <span>${data.serialNumber}</span></div>
                <div class="settings-item"><span>ความจุแบตเตอรี่</span> <span>${data.batteryMah} mAh (${data.batteryLevel}%)</span></div>
            </div>
        </div>
    `;
}

async function toggleSettingWifi() {
    pulsePillar('pillarEncapsulation');
    await sendAppAction("TOGGLE_WIFI", {});
}

async function toggleSettingCellular() {
    pulsePillar('pillarEncapsulation');
    await sendAppAction("TOGGLE_CELLULAR", {});
}

async function setBrightness(val) {
    pulsePillar('pillarEncapsulation');
    await sendAppAction("SET_BRIGHTNESS", { brightness: val });
}

async function setVolume(val) {
    pulsePillar('pillarEncapsulation');
    await sendAppAction("SET_VOLUME", { volume: val });
}

// Notification Shade Logic
function toggleNotificationShade() {
    const shade = document.getElementById("notificationShade");
    shade.classList.toggle("open");
}

function renderNotificationShade(notifs) {
    const list = document.getElementById("shadeNotificationList");
    if (!notifs || notifs.length === 0) {
        list.innerHTML = '<p class="empty-shade">ไม่มีการแจ้งเตือน</p>';
        return;
    }
    list.innerHTML = notifs.map(n => `
        <div class="shade-item">
            <div class="shade-item-title">${escapeHtml(n.title)}</div>
            <div class="shade-item-msg">${escapeHtml(n.message)}</div>
            <div class="shade-item-app">จาก: ${escapeHtml(n.appSource)} &bull; ${new Date(n.timestamp).toLocaleTimeString()}</div>
        </div>
    `).join('');
}

async function clearNotifications() {
    await fetch("/api/notifications/clear", { method: "POST" });
    fetchPhoneState();
}

// Render OOP Inspector & Heap Data
function renderInspector(state) {
    if (!state) return;

    // Battery
    if (state.battery) {
        const isInf = state.battery.isInfinite;
        document.getElementById("inspectBattery").textContent = isInf ? "100% ∞ (Infinite)" : `${state.battery.level}%`;
        document.getElementById("inspectBatteryMah").textContent = `${state.battery.capacityMah} mAh (พลังงานไม่มีวันหมด &bull; Temp: ${state.battery.temperatureCelsius}°C)`;
    }

    // Storage
    if (state.storage) {
        document.getElementById("inspectStorage").textContent = `${state.storage.freeMb.toLocaleString()} MB Free`;
        document.getElementById("inspectStorageUsed").textContent = `Used: ${state.storage.usedMb.toLocaleString()} MB (${state.storage.usedPercent}%)`;

        // Files table
        const filesTbody = document.getElementById("storageFilesList");
        const files = state.storage.files || [];
        if (files.length === 0) {
            filesTbody.innerHTML = '<tr><td colspan="3" style="text-align:center;">ไม่มีไฟล์ที่บันทึกไว้</td></tr>';
        } else {
            filesTbody.innerHTML = files.slice(0, 5).map(f => `
                <tr>
                    <td><strong>${escapeHtml(f.fileName)}</strong></td>
                    <td>${f.sizeMb} MB</td>
                    <td><span style="color:#94a3b8;">${escapeHtml(f.fileType)}</span></td>
                </tr>
            `).join('');
        }
    }

    // Active App
    const inspectApp = document.getElementById("inspectApp");
    const inspectAppState = document.getElementById("inspectAppState");
    if (state.runningApp) {
        inspectApp.textContent = `${state.runningApp.appName} (${state.runningApp.appId})`;
        inspectAppState.textContent = `Lifecycle: ${state.runningApp.state}`;
    } else {
        inspectApp.textContent = "Home Screen (Launcher)";
        inspectAppState.textContent = "Lifecycle: IDLE";
    }

    // Network
    if (state.network) {
        document.getElementById("inspectNetwork").textContent = state.network.connectionType;
        document.getElementById("inspectSignal").textContent = `Signal Strength: ${state.network.signalStrength}%`;
    }
}

// Phone Actions (Hybrid: Java Localhost API with Cloud Fallback)
async function togglePower() {
    pulsePillar('pillarEncapsulation');
    if (isOfflineMode) {
        clientSimState.device.isPoweredOn = !clientSimState.device.isPoweredOn;
        clientSimState.display.isScreenOn = clientSimState.device.isPoweredOn;
        renderPhoneUI(clientSimState);
        renderInspector(clientSimState);
        return;
    }
    try {
        await fetch("/api/power", { method: "POST" });
        fetchPhoneState();
    } catch(e) {
        enableClientSimulation();
    }
}

async function changeVolume(direction) {
    pulsePillar('pillarEncapsulation');
    if (isOfflineMode) {
        const step = direction === 'up' ? 10 : -10;
        clientSimState.device.volume = Math.max(0, Math.min(100, (clientSimState.device.volume || 70) + step));
        renderInspector(clientSimState);
        return;
    }
    try {
        await fetch(`/api/volume/${direction}`, { method: "POST" });
        fetchPhoneState();
    } catch(e) {
        enableClientSimulation();
    }
}

async function chargeBattery(amount) {
    pulsePillar('pillarEncapsulation');
    if (isOfflineMode) {
        clientSimState.battery.level = 100;
        renderPhoneUI(clientSimState);
        renderInspector(clientSimState);
        return;
    }
    try {
        await fetch("/api/battery/charge", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ amount: String(amount) })
        });
        fetchPhoneState();
    } catch(e) {
        enableClientSimulation();
    }
}

async function launchApp(appId) {
    lastRenderedAppKey = "";
    pulsePillar('pillarInheritance');
    pulsePillar('pillarPolymorphism');
    if (isOfflineMode) {
        if (appId === "com.psru.lostfound") {
            const prevData = (clientSimState.runningApp && clientSimState.runningApp.appData) || {};
            clientSimState.runningApp = {
                appId: "com.psru.lostfound",
                appName: "PSRU Lost & Found",
                state: "RESUMED",
                appData: {
                    isLoggedIn: prevData.isLoggedIn === true,
                    currentUser: prevData.isLoggedIn ? prevData.currentUser : null,
                    items: simPosts,
                    posts: simPosts
                }
            };
        } else if (appId === "com.android.settings") {
            clientSimState.runningApp = {
                appId: "com.android.settings",
                appName: "การตั้งค่า (Settings)",
                state: "RESUMED",
                appData: {
                    brand: "Google / PSRU",
                    model: "Pixel 9 Pro",
                    androidVersion: "Android 15",
                    apiLevel: 35,
                    kernel: "6.1.75-android15-g1234",
                    serialNumber: "SN-6812247050",
                    batteryMah: 5000,
                    batteryLevel: (clientSimState.battery && clientSimState.battery.level) || 100,
                    brightness: (clientSimState.display && clientSimState.display.brightness) || 85,
                    volume: (clientSimState.device && clientSimState.device.volume) || 80,
                    wifiEnabled: true,
                    cellularEnabled: true
                }
            };
        } else if (appId === "com.android.dialer") {
            clientSimState.runningApp = {
                appId: "com.android.dialer",
                appName: "โทรศัพท์ (Phone)",
                state: "RESUMED",
                appData: {}
            };
        } else if (appId === "com.android.camera") {
            clientSimState.runningApp = {
                appId: "com.android.camera",
                appName: "กล้องถ่ายรูป (Camera)",
                state: "RESUMED",
                appData: {}
            };
        } else if (appId === "com.android.calc") {
            clientSimState.runningApp = {
                appId: "com.android.calc",
                appName: "เครื่องคิดเลข (Calculator)",
                state: "RESUMED",
                appData: {}
            };
        } else if (appId === "com.android.notes") {
            clientSimState.runningApp = {
                appId: "com.android.notes",
                appName: "สมุดบันทึก (Notes)",
                state: "RESUMED",
                appData: {}
            };
        }
        renderPhoneUI(clientSimState);
        renderInspector(clientSimState);
        return;
    }
    if (appId === "com.android.dialer" || appId === "com.android.camera" || appId === "com.android.calc" || appId === "com.android.notes") {
        const appNames = {
            "com.android.dialer": "โทรศัพท์ (Phone)",
            "com.android.camera": "กล้องถ่ายรูป (Camera)",
            "com.android.calc": "เครื่องคิดเลข (Calculator)",
            "com.android.notes": "สมุดบันทึก (Notes)"
        };
        const appObj = {
            appId: appId,
            appName: appNames[appId] || "App",
            state: "RESUMED",
            appData: {}
        };
        if (currentPhoneState) currentPhoneState.runningApp = appObj;
        if (clientSimState) clientSimState.runningApp = appObj;
        renderPhoneUI(currentPhoneState || clientSimState);
        return;
    }
    try {
        await fetch("/api/app/launch", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ appId: appId })
        });
        fetchPhoneState();
    } catch (err) {
        enableClientSimulation();
    }
}

async function pressHome() {
    lastRenderedAppKey = "";
    pulsePillar('pillarPolymorphism');
    if (isOfflineMode) {
        clientSimState.runningApp = null;
        renderPhoneUI(clientSimState);
        renderInspector(clientSimState);
        return;
    }
    try {
        await fetch("/api/home", { method: "POST" });
        fetchPhoneState();
    } catch(e) {
        enableClientSimulation();
    }
}

async function pressBack() {
    lastRenderedAppKey = "";
    pulsePillar('pillarPolymorphism');
    if (isOfflineMode) {
        clientSimState.runningApp = null;
        renderPhoneUI(clientSimState);
        renderInspector(clientSimState);
        return;
    }
    try {
        await fetch("/api/back", { method: "POST" });
        fetchPhoneState();
    } catch(e) {
        enableClientSimulation();
    }
}

async function pressRecents() {
    alert("Task Manager: แสดงรายชื่อแอปพลิเคชันล่าสุดในหน่วยความจำ");
}

async function sendAppAction(action, params) {
    if (isOfflineMode) {
        const res = executeSimAction(action, params);
        renderPhoneUI(clientSimState);
        renderInspector(clientSimState);
        return res;
    }
    try {
        const payload = Object.assign({ action: action }, params);
        const res = await fetch("/api/app/action", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error("HTTP " + res.status);
        const jsonResult = await res.json();
        
        // Also replicate to Firebase Cloud Firestore for real-time multi-device sync
        if (firestoreDb && jsonResult.success) {
            try {
                if (action === "CREATE_POST" && jsonResult.data) {
                    const postObj = jsonResult.data.post || jsonResult.data;
                    if (postObj && postObj.id) {
                        firestoreDb.collection("posts").doc(postObj.id).set(postObj);
                    }
                } else if (action === "RESOLVE_POST" && params.itemId) {
                    firestoreDb.collection("posts").doc(params.itemId).update({ status: "RESOLVED" });
                } else if (action === "DELETE_POST" && params.itemId) {
                    firestoreDb.collection("posts").doc(params.itemId).delete().catch(e => {});
                    firestoreDb.collection("posts").where("id", "==", params.itemId).get().then(s => s.forEach(d => d.ref.delete())).catch(e => {});
                } else if (action === "ADMIN_BROADCAST") {
                    const bc = (jsonResult.data && (jsonResult.data.broadcast || jsonResult.data)) || {
                        id: "BC-" + Date.now(),
                        title: params.title || "ประกาศจากผู้ดูแลระบบ",
                        message: params.message || "",
                        senderName: "Super Admin (พีรนาท)",
                        senderId: "6812247050",
                        createdAt: Date.now()
                    };
                    firestoreDb.collection("broadcasts").doc(bc.id).set(bc).catch(e => {});
                } else if (action === "ADMIN_DELETE_BROADCAST" && params.broadcastId) {
                    firestoreDb.collection("broadcasts").doc(params.broadcastId).delete().catch(e => {});
                    firestoreDb.collection("broadcasts").where("id", "==", params.broadcastId).get().then(s => s.forEach(d => d.ref.delete())).catch(e => {});
                } else if (action === "ADMIN_CLEAR_RESOLVED") {
                    firestoreDb.collection("posts").where("status", "==", "RESOLVED").get().then(s => s.forEach(d => d.ref.delete())).catch(e => {});
                } else if (action === "REGISTER" && jsonResult.data && jsonResult.data.user) {
                    firestoreDb.collection("users").doc(jsonResult.data.user.studentId).set(jsonResult.data.user);
                }
            } catch (e) {
                console.warn("Firebase shadow sync error:", e);
            }
        }

        if (jsonResult.success) {
            if (action === "LOGIN" || action === "RESTORE_SESSION") {
                const u = (jsonResult.data && (jsonResult.data.user || jsonResult.data)) || null;
                if (u) saveSessionUser(u);
            } else if (action === "REGISTER") {
                const u = (jsonResult.data && (jsonResult.data.user || jsonResult.data)) || null;
                if (u) saveSessionUser(u);
            } else if (action === "LOGOUT") {
                clearSessionUser();
            }
        }
        
        fetchPhoneState();
        return jsonResult;
    } catch (err) {
        isOfflineMode = true;
        if (!clientSimState) enableClientSimulation();
        const res = executeSimAction(action, params);
        renderPhoneUI(clientSimState);
        renderInspector(clientSimState);
        return res;
    }
}

function pulsePillar(elementId) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.classList.add("active-pulse");
    setTimeout(() => el.classList.remove("active-pulse"), 800);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function toggleInspector() {
    const layout = document.getElementById("appLayout");
    const btn = document.getElementById("btnToggleInspector");
    if (!layout) return;

    layout.classList.toggle("phone-only-mode");
    const isPhoneOnly = layout.classList.contains("phone-only-mode");
    if (btn) {
        btn.innerHTML = isPhoneOnly ? '<span>🔬 แสดงข้อมูล OOP</span>' : '<span>📱 ซ่อนแผง OOP (แสดงเฉพาะมือถือ)</span>';
    }
}


// ============================================================================
// DATABASE VIEWER MODAL LOGIC
// ============================================================================

let currentDatabaseData = null;
let currentDbTab = 'users'; // 'users', 'posts', 'storage', 'raw'

async function openDatabaseModal() {
    const overlay = document.getElementById("dbModalOverlay");
    if (overlay) overlay.classList.add("active");
    await fetchAndRenderDatabase();
}

function closeDatabaseModal(e) {
    if (e && e.target && e.target.id !== "dbModalOverlay" && !e.target.classList.contains("btn-db-close")) {
        return;
    }
    const overlay = document.getElementById("dbModalOverlay");
    if (overlay) overlay.classList.remove("active");
}

function switchDbTab(tab) {
    currentDbTab = tab;
    document.querySelectorAll(".db-tab-btn").forEach(btn => btn.classList.remove("active"));
    if (tab === 'users') document.getElementById("tabBtnUsers")?.classList.add("active");
    if (tab === 'posts') document.getElementById("tabBtnPosts")?.classList.add("active");
    if (tab === 'storage') document.getElementById("tabBtnStorage")?.classList.add("active");
    if (tab === 'raw') document.getElementById("tabBtnRaw")?.classList.add("active");
    renderDatabaseTabContent();
}

async function fetchAndRenderDatabase() {
    const bodyEl = document.getElementById("dbModalBody");
    if (!bodyEl) return;

    if (isOfflineMode) {
        currentDatabaseData = {
            users: simUsers.map(u => ({
                studentId: u.studentId,
                fullName: u.fullName,
                faculty: u.faculty,
                instagram: u.instagram,
                hasProfilePhoto: !!u.profileImage,
                registeredAt: u.registeredAt
            })),
            posts: simPosts,
            storageFiles: (clientSimState && clientSimState.storage) ? clientSimState.storage.files : [],
            metadata: {
                storageEngine: "In-Memory OOP Simulation (Client Standalone)",
                usersCollection: "Map<String, StudentUser> userDatabase",
                postsCollection: "CopyOnWriteArrayList<LostFoundItem> itemList",
                hardwareStorage: "Storage.java (Simulated Flash Storage)",
                timestamp: Date.now()
            }
        };

        const uCount = document.getElementById("dbUsersCount");
        if (uCount) uCount.textContent = (currentDatabaseData.users || []).length;
        const pCount = document.getElementById("dbPostsCount");
        if (pCount) pCount.textContent = (currentDatabaseData.posts || []).length;
        const sCount = document.getElementById("dbStorageCount");
        if (sCount) sCount.textContent = (currentDatabaseData.storageFiles || []).length;

        renderDatabaseTabContent();
        return;
    }

    try {
        bodyEl.innerHTML = '<div class="db-loading">กำลังเชื่อมต่อกับ Java Localhost Server...</div>';
        const res = await fetch("/api/database");
        if (!res.ok) throw new Error("HTTP error " + res.status);
        const data = await res.json();
        currentDatabaseData = data;

        // Update counts
        const uCount = document.getElementById("dbUsersCount");
        if (uCount) uCount.textContent = (data.users || []).length;
        const pCount = document.getElementById("dbPostsCount");
        if (pCount) pCount.textContent = (data.posts || []).length;
        const sCount = document.getElementById("dbStorageCount");
        if (sCount) sCount.textContent = (data.storageFiles || []).length;

        renderDatabaseTabContent();
    } catch (err) {
        isOfflineMode = true;
        fetchAndRenderDatabase();
    }
}

function renderDatabaseTabContent() {
    const bodyEl = document.getElementById("dbModalBody");
    if (!bodyEl || !currentDatabaseData) return;

    const data = currentDatabaseData;

    if (currentDbTab === 'users') {
        const users = data.users || [];
        if (users.length === 0) {
            bodyEl.innerHTML = '<div class="db-empty">ไม่มีข้อมูลผู้ใช้งานในระบบ</div>';
            return;
        }
        bodyEl.innerHTML = `
            <div class="db-table-wrapper">
                <div class="db-table-caption">
                    <span>📌 คอลเลกชันผู้ใช้งาน: <code>Map&lt;String, StudentUser&gt; userDatabase</code> ใน AuthController</span>
                </div>
                <table class="db-table">
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>รหัสนักศึกษา</th>
                            <th>ชื่อ - นามสกุล</th>
                            <th>คณะสังกัด</th>
                            <th>Instagram (IG)</th>
                            <th>รูปโปรไฟล์</th>
                            <th>เวลาลงทะเบียน</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${users.map((u, idx) => `
                            <tr>
                                <td>${idx + 1}</td>
                                <td><strong><code>${escapeHtml(u.studentId)}</code></strong></td>
                                <td>${escapeHtml(u.fullName)}</td>
                                <td><span class="db-tag-faculty">${escapeHtml(u.faculty)}</span></td>
                                <td><span class="db-tag-ig">${escapeHtml(u.instagram || '-')}</span></td>
                                <td>${u.hasProfilePhoto ? '✅ มีรูปถ่าย' : '❌ ไม่มี'}</td>
                                <td>${new Date(u.registeredAt).toLocaleString('th-TH')}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } else if (currentDbTab === 'posts') {
        const posts = data.posts || [];
        if (posts.length === 0) {
            bodyEl.innerHTML = '<div class="db-empty">ไม่มีประกาศในระบบ</div>';
            return;
        }
        bodyEl.innerHTML = `
            <div class="db-table-wrapper">
                <div class="db-table-caption">
                    <span>📌 คอลเลกชันประกาศ: <code>CopyOnWriteArrayList&lt;LostFoundItem&gt; itemList</code> ใน PsruLostFoundApp</span>
                </div>
                <table class="db-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>ประเภท</th>
                            <th>ชื่อสิ่งของ</th>
                            <th>หมวดหมู่</th>
                            <th>สถานที่</th>
                            <th>ผู้แจ้ง</th>
                            <th>Instagram</th>
                            <th>สถานะ</th>
                            <th>เวลาที่โพสต์</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${posts.map(p => `
                            <tr>
                                <td><code>${escapeHtml(p.id)}</code></td>
                                <td>
                                    <span class="db-badge ${p.type === 'LOST' ? 'db-badge-lost' : 'db-badge-found'}">
                                        ${p.type === 'LOST' ? '🔴 ของหาย' : '🟢 พบของ'}
                                    </span>
                                </td>
                                <td><strong>${escapeHtml(p.title)}</strong></td>
                                <td>${escapeHtml(p.category || '-')}</td>
                                <td>📍 ${escapeHtml(p.location || '-')}</td>
                                <td>${escapeHtml(p.reporterName)} (${escapeHtml(p.reporterId)})</td>
                                <td><span class="db-tag-ig">${escapeHtml(p.reporterIg || '-')}</span></td>
                                <td>
                                    <span class="db-status-tag ${p.status === 'RESOLVED' ? 'status-done' : 'status-pending'}">
                                        ${p.status === 'RESOLVED' ? '✅ ส่งคืนแล้ว' : '⏳ กำลังค้นหา'}
                                    </span>
                                </td>
                                <td>${new Date(p.createdAt).toLocaleTimeString('th-TH', { hour: '2-digit', minute: '2-digit' })}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } else if (currentDbTab === 'storage') {
        const files = data.storageFiles || [];
        bodyEl.innerHTML = `
            <div class="db-table-wrapper">
                <div class="db-table-caption">
                    <span>📌 ฮาร์ดแวร์จัดเก็บข้อมูลจำลอง: <code>Storage.java</code> (Internal Flash Storage)</span>
                </div>
                <table class="db-table">
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>ชื่อไฟล์</th>
                            <th>ขนาด (MB)</th>
                            <th>MIME Type</th>
                            <th>สร้างเมื่อ</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${files.map((f, idx) => `
                            <tr>
                                <td>${idx + 1}</td>
                                <td><strong><code>${escapeHtml(f.fileName)}</code></strong></td>
                                <td>${f.sizeMb} MB</td>
                                <td><code>${escapeHtml(f.fileType)}</code></td>
                                <td>${new Date(f.createdAt).toLocaleTimeString('th-TH')}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } else if (currentDbTab === 'raw') {
        bodyEl.innerHTML = `
            <div class="db-raw-wrapper">
                <div class="db-raw-actions">
                    <span>Endpoint: <code>http://localhost:8088/api/database</code></span>
                    <button class="btn-copy-json" onclick="navigator.clipboard.writeText(JSON.stringify(currentDatabaseData, null, 2)); alert('คัดลอก JSON เรียบร้อยแล้ว!');">📋 คัดลอก JSON</button>
                </div>
                <pre class="db-json-view">${escapeHtml(JSON.stringify(data, null, 2))}</pre>
            </div>
        `;
    }
}


// ============================================================================
// SYSTEM UTILITY APPS (DIALER, CAMERA, CALCULATOR, NOTES)
// ============================================================================

// 1. DIALER APP
function renderDialerAppHtml() {
    return `
        <div class="dialer-screen">
            <div class="dialer-display" id="dialDisplay">${dialBuffer || '<span style="color:#64748b; font-size:1.1rem; font-weight:normal;">กดเบอร์โทรศัพท์...</span>'}</div>
            <div class="dialpad-grid">
                <button class="dial-key" onclick="pressDialDigit('1')">1<span class="sub">&nbsp;</span></button>
                <button class="dial-key" onclick="pressDialDigit('2')">2<span class="sub">ABC</span></button>
                <button class="dial-key" onclick="pressDialDigit('3')">3<span class="sub">DEF</span></button>
                <button class="dial-key" onclick="pressDialDigit('4')">4<span class="sub">GHI</span></button>
                <button class="dial-key" onclick="pressDialDigit('5')">5<span class="sub">JKL</span></button>
                <button class="dial-key" onclick="pressDialDigit('6')">6<span class="sub">MNO</span></button>
                <button class="dial-key" onclick="pressDialDigit('7')">7<span class="sub">PQRS</span></button>
                <button class="dial-key" onclick="pressDialDigit('8')">8<span class="sub">TUV</span></button>
                <button class="dial-key" onclick="pressDialDigit('9')">9<span class="sub">WXYZ</span></button>
                <button class="dial-key" onclick="pressDialDigit('*')">*<span class="sub">&nbsp;</span></button>
                <button class="dial-key" onclick="pressDialDigit('0')">0<span class="sub">+</span></button>
                <button class="dial-key" onclick="pressDialDigit('#')">#<span class="sub">&nbsp;</span></button>
            </div>
            <div class="dial-actions">
                <button onclick="clearDialDigit()" style="background:none; border:none; color:#94a3b8; font-size:1.3rem; cursor:pointer; padding:10px;">⌫</button>
                <button class="call-btn" onclick="executeDialCall()">📞</button>
            </div>
            <div class="call-history-box" style="margin-top:10px; text-align:center; color:#94a3b8;">
                เบอร์ฉุกเฉิน มรภ.พิบูลสงคราม: <strong style="color:#38bdf8;">055-267000</strong>
            </div>
        </div>
    `;
}

function pressDialDigit(d) {
    dialBuffer += d;
    const el = document.getElementById("dialDisplay");
    if (el) el.textContent = dialBuffer;
}

function clearDialDigit() {
    dialBuffer = dialBuffer.slice(0, -1);
    const el = document.getElementById("dialDisplay");
    if (el) {
        el.innerHTML = dialBuffer || '<span style="color:#64748b; font-size:1.1rem; font-weight:normal;">กดเบอร์โทรศัพท์...</span>';
    }
}

function executeDialCall() {
    if (!dialBuffer) {
        alert("กรุณากดหมายเลขโทรศัพท์ก่อนโทรออก");
        return;
    }
    const notif = {
        title: "กำลังโทรออก: " + dialBuffer,
        message: "การโทรเชื่อมต่อสำเร็จผ่านเครือข่าย Wi-Fi Calling",
        appSource: "Dialer",
        timestamp: Date.now()
    };
    if (clientSimState) {
        clientSimState.notifications.unshift(notif);
        renderPhoneUI(clientSimState);
    }
    alert("📞 กำลังโทรออกไปยัง " + dialBuffer + " ...");
}

// 2. CAMERA APP
let currentCameraSubject = "🎒";
const cameraSubjects = ["🎒", "📱", "💳", "🔑", "🎧", "💻", "⌚"];

function renderCameraAppHtml() {
    return `
        <div style="display:flex; flex-direction:column; height:100%; justify-content:space-between;">
            <div class="camera-viewfinder" id="cameraViewfinder">
                <div class="viewfinder-grid">
                    <div class="grid-line"></div><div class="grid-line"></div><div></div>
                    <div class="grid-line"></div><div class="grid-line"></div><div></div>
                </div>
                <div class="animated-camera-subject" id="cameraSubject">${currentCameraSubject}</div>
                <div class="flash-screen" id="cameraFlash"></div>
            </div>
            <div style="text-align:center; font-size:0.75rem; color:#94a3b8; margin-top:8px;">
                มุมมองกล้อง 4K HDR • ถ่ายภาพสิ่งของเพื่อลงประกาศของหาย
            </div>
            <div class="camera-controls">
                <div class="camera-preview-thumb" id="cameraThumb">🖼️</div>
                <button class="shutter-btn" onclick="takeCameraSnapshot()"></button>
                <button onclick="switchCameraSubject()" style="background:#1e293b; border:1px solid #334155; color:#fff; width:44px; height:44px; border-radius:50%; font-size:1.1rem; cursor:pointer;" title="เปลี่ยนวัตถุ">🔄</button>
            </div>
        </div>
    `;
}

function switchCameraSubject() {
    const nextIdx = (cameraSubjects.indexOf(currentCameraSubject) + 1) % cameraSubjects.length;
    currentCameraSubject = cameraSubjects[nextIdx];
    const el = document.getElementById("cameraSubject");
    if (el) el.textContent = currentCameraSubject;
}

function takeCameraSnapshot() {
    const flash = document.getElementById("cameraFlash");
    if (flash) {
        flash.style.opacity = "0.8";
        setTimeout(() => { flash.style.opacity = "0"; }, 150);
    }
    const notif = {
        title: "📸 ถ่ายภาพสิ่งของสำเร็จ",
        message: "บันทึกภาพ " + currentCameraSubject + " ลงหน่วยความจำเครื่องสมาร์ตโฟนแล้ว",
        appSource: "Camera",
        timestamp: Date.now()
    };
    if (clientSimState) {
        clientSimState.notifications.unshift(notif);
        renderPhoneUI(clientSimState);
    }
    const thumb = document.getElementById("cameraThumb");
    if (thumb) thumb.textContent = currentCameraSubject;
}

// 3. CALCULATOR APP
function renderCalcAppHtml() {
    return `
        <div style="display:flex; flex-direction:column; height:100%;">
            <div class="calc-display">
                <div class="calc-expr" id="calcExprDisplay">&nbsp;</div>
                <div class="calc-res" id="calcResultDisplay">${calcBuffer || '0'}</div>
            </div>
            <div class="calc-grid">
                <button class="calc-btn" style="color:#ef4444;" onclick="pressCalcKey('C')">C</button>
                <button class="calc-btn" style="color:#38bdf8;" onclick="pressCalcKey('(')">(</button>
                <button class="calc-btn" style="color:#38bdf8;" onclick="pressCalcKey(')')">)</button>
                <button class="calc-btn" style="color:#f59e0b;" onclick="pressCalcKey('/')">÷</button>

                <button class="calc-btn" onclick="pressCalcKey('7')">7</button>
                <button class="calc-btn" onclick="pressCalcKey('8')">8</button>
                <button class="calc-btn" onclick="pressCalcKey('9')">9</button>
                <button class="calc-btn" style="color:#f59e0b;" onclick="pressCalcKey('*')">×</button>

                <button class="calc-btn" onclick="pressCalcKey('4')">4</button>
                <button class="calc-btn" onclick="pressCalcKey('5')">5</button>
                <button class="calc-btn" onclick="pressCalcKey('6')">6</button>
                <button class="calc-btn" style="color:#f59e0b;" onclick="pressCalcKey('-')">-</button>

                <button class="calc-btn" onclick="pressCalcKey('1')">1</button>
                <button class="calc-btn" onclick="pressCalcKey('2')">2</button>
                <button class="calc-btn" onclick="pressCalcKey('3')">3</button>
                <button class="calc-btn" style="color:#f59e0b;" onclick="pressCalcKey('+')">+</button>

                <button class="calc-btn" style="grid-column: span 2;" onclick="pressCalcKey('0')">0</button>
                <button class="calc-btn" onclick="pressCalcKey('.')">.</button>
                <button class="calc-btn" style="background:#00e676; color:#000; font-weight:700;" onclick="pressCalcKey('=')">=</button>
            </div>
        </div>
    `;
}

function pressCalcKey(key) {
    const resEl = document.getElementById("calcResultDisplay");
    const exprEl = document.getElementById("calcExprDisplay");

    if (key === 'C') {
        calcBuffer = "0";
        if (exprEl) exprEl.innerHTML = "&nbsp;";
    } else if (key === '=') {
        try {
            const cleanExpr = calcBuffer.replace(/×/g, '*').replace(/÷/g, '/');
            // Safe evaluation using simple Function
            const result = Function('"use strict";return (' + cleanExpr + ')')();
            if (exprEl) exprEl.textContent = calcBuffer + " =";
            calcBuffer = String(result);
        } catch(e) {
            calcBuffer = "Error";
        }
    } else {
        if (calcBuffer === "0" || calcBuffer === "Error") {
            calcBuffer = key;
        } else {
            calcBuffer += key;
        }
    }
    if (resEl) resEl.textContent = calcBuffer;
}

// 4. NOTES APP
function renderNotesAppHtml() {
    const savedNotes = JSON.parse(localStorage.getItem("psru_notes") || "[]");
    return `
        <div style="display:flex; flex-direction:column; height:100%;">
            <div class="note-input-box">
                <input type="text" id="noteTitleInput" placeholder="หัวข้อโน้ต เช่น ข้อมูลสิ่งของที่หาย..." />
                <textarea id="noteContentInput" rows="3" placeholder="รายละเอียดบันทึกช่วยจำ..."></textarea>
                <button class="btn-portal-submit" onclick="handleSaveNote()" style="padding:8px 0; font-size:0.75rem;">
                    ➕ บันทึกโน้ตใหม่
                </button>
            </div>
            <div class="notes-list" id="notesList" style="max-height:220px; overflow-y:auto;">
                ${savedNotes.length === 0 ? '<div style="text-align:center; color:#64748b; font-size:0.75rem; padding:20px;">ยังไม่มีบันทึกช่วยจำ</div>' : 
                    savedNotes.map((n, idx) => `
                        <div class="note-card">
                            <div>
                                <div class="note-card-title">${escapeHtml(n.title)}</div>
                                <div class="note-card-body">${escapeHtml(n.body)}</div>
                            </div>
                            <button onclick="handleDeleteNote(${idx})" style="background:none; border:none; color:#ef4444; cursor:pointer; font-size:0.9rem; padding:4px;">✕</button>
                        </div>
                    `).join('')
                }
            </div>
        </div>
    `;
}

function handleSaveNote() {
    const titleEl = document.getElementById("noteTitleInput");
    const contentEl = document.getElementById("noteContentInput");
    if (!titleEl || !contentEl) return;
    const title = titleEl.value.trim();
    const body = contentEl.value.trim();
    if (!title) {
        alert("กรุณากรอกหัวข้อโน้ต");
        return;
    }
    const notes = JSON.parse(localStorage.getItem("psru_notes") || "[]");
    notes.unshift({ title, body, date: Date.now() });
    localStorage.setItem("psru_notes", JSON.stringify(notes));
    lastRenderedAppKey = "";
    if (clientSimState && clientSimState.runningApp) {
        renderActiveApp(clientSimState.runningApp);
    }
}

function handleDeleteNote(idx) {
    const notes = JSON.parse(localStorage.getItem("psru_notes") || "[]");
    notes.splice(idx, 1);
    localStorage.setItem("psru_notes", JSON.stringify(notes));
    lastRenderedAppKey = "";
    if (clientSimState && clientSimState.runningApp) {
        renderActiveApp(clientSimState.runningApp);
    }
}


// ============================================================================
// ADMIN MANAGEMENT PANEL (PSRU LOST & FOUND ADMIN)
// ============================================================================

function renderPsruAdminTab(data) {
    const user = data.currentUser || {};
    const rawItems = (data.items && data.items.length > 0) ? data.items : ((data.posts && data.posts.length > 0) ? data.posts : (simPosts || []));
    const allUsers = (data.allUsers && data.allUsers.length > 0) ? data.allUsers : (simUsers || []);

    const totalUsers = allUsers.length;
    const totalPosts = rawItems.length;
    const lostCount = rawItems.filter(p => p.type === 'LOST').length;
    const foundCount = rawItems.filter(p => p.type === 'FOUND').length;
    const resolvedCount = rawItems.filter(p => p.status === 'RESOLVED').length;

    return `
        <div class="admin-panel">
            <!-- Admin Header -->
            <div class="admin-header-card">
                <div>
                    <div class="admin-name">👑 ${escapeHtml(user.fullName || 'พีรนาท สิงห์โต')}</div>
                    <div class="admin-id">Super Admin (รหัส: <code>${escapeHtml(user.studentId || '6812247050')}</code>)</div>
                </div>
                <div class="admin-badge-gold">ADMIN ACTIVE</div>
            </div>

            <!-- System Metrics Overview -->
            <div class="admin-metrics-grid">
                <div class="admin-metric-card">
                    <div class="num">${totalUsers}</div>
                    <div class="lbl">👥 นักศึกษาในระบบ</div>
                </div>
                <div class="admin-metric-card">
                    <div class="num">${totalPosts}</div>
                    <div class="lbl">📋 ประกาศทั้งหมด</div>
                </div>
                <div class="admin-metric-card">
                    <div class="num" style="color: #ef4444;">${lostCount}</div>
                    <div class="lbl">🔴 กำลังตามหา</div>
                </div>
                <div class="admin-metric-card">
                    <div class="num" style="color: #10b981;">${resolvedCount}</div>
                    <div class="lbl">✅ ส่งคืนสำเร็จ</div>
                </div>
            </div>

            <!-- Admin Push Broadcast Notification -->
            <div class="admin-section">
                <div class="admin-section-title">📢 ส่งประกาศด่วน (Push Notification Broadcast)</div>
                <div class="admin-broadcast-box">
                    <input type="text" id="adminBroadcastTitle" placeholder="หัวข้อประกาศด่วน เช่น ประกาศจากงานกิจการนักศึกษา..." style="width:100%; background:#111726; border:1px solid #1e293b; border-radius:8px; color:#f1f5f9; padding:8px 10px; font-size:0.75rem; margin-bottom:6px; box-sizing:border-box;" />
                    <textarea id="adminBroadcastMsg" rows="2" placeholder="พิมพ์ข้อความที่ต้องการแจ้งเตือนไปยังสมาร์ตโฟนทุกคน..."></textarea>
                    <button class="btn-admin-broadcast" onclick="handleAdminBroadcast()">
                        ⚡ ยิงการแจ้งเตือนทันที (Broadcast to All Devices)
                    </button>
                </div>
                ${(activeBroadcasts && activeBroadcasts.length > 0) ? `
                    <div style="margin-top: 10px;">
                        <div style="font-size: 0.7rem; font-weight: 700; color: #f59e0b; margin-bottom: 6px;">
                            📢 ประกาศด่วนที่กำลังแสดงผล (${activeBroadcasts.length})
                        </div>
                        ${activeBroadcasts.map(bc => `
                            <div class="admin-broadcast-item">
                                <div class="admin-item-info">
                                    <div class="admin-item-title">${escapeHtml(bc.title || 'ประกาศด่วน')}</div>
                                    <div class="admin-item-sub">${escapeHtml(bc.message)}</div>
                                    <div class="admin-item-sub" style="color:#f59e0b; font-size:0.6rem;">
                                        โดย: ${escapeHtml(bc.senderName || 'Admin')} • ${formatPostTime(bc.createdAt)}
                                    </div>
                                </div>
                                <button class="btn-admin-action btn-admin-del" onclick="handleAdminDeleteBroadcast('${escapeHtml(bc.id)}')" title="ลบประกาศด่วนนี้">
                                    🗑️
                                </button>
                            </div>
                        `).join('')}
                    </div>
                ` : ''}
            </div>

            <!-- Post Management List -->
            <div class="admin-section">
                <div class="admin-section-title">
                    <span>📋 จัดการโพสต์ประกาศทั้งหมดในระบบ (${rawItems.length})</span>
                </div>
                <div class="admin-users-list" style="max-height: 250px; overflow-y: auto;">
                    ${rawItems.length === 0 ? `
                        <div style="font-size: 0.72rem; color: #64748b; text-align: center; padding: 15px;">
                            ไม่มีโพสต์ประกาศในระบบ (ลบหมดแล้ว)
                        </div>
                    ` : rawItems.map(item => `
                        <div class="admin-post-item">
                            <div class="admin-item-info">
                                <div class="admin-item-title">
                                    <span style="font-size:0.65rem; color:${item.type === 'LOST' ? '#ef4444' : '#10b981'}; font-weight:800;">
                                        [${item.type === 'LOST' ? 'ของหาย' : 'แจ้งพบ'}]
                                    </span>
                                    ${escapeHtml(item.title)}
                                </div>
                                <div class="admin-item-sub">
                                    ผู้แจ้ง: <code>${escapeHtml(item.reporterName)}</code> (${escapeHtml(item.reporterId)})
                                </div>
                                <div class="admin-item-sub">
                                    สถานะ: <span style="color:${item.status === 'RESOLVED' ? '#10b981' : '#f59e0b'}; font-weight:700;">${item.status === 'RESOLVED' ? '✅ ส่งคืนแล้ว' : '⏳ กำลังค้นหา'}</span> • ${formatPostTime(item.createdAt)}
                                </div>
                            </div>
                            <div class="admin-btn-group">
                                <button class="btn-admin-action" style="background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.4);" onclick="handleAdminPinPost('${escapeHtml(item.id)}')" title="ปักหมุดขึ้นบนสุด">
                                    📌
                                </button>
                                ${item.status !== 'RESOLVED' ? `
                                    <button class="btn-admin-action" style="background: rgba(16, 185, 129, 0.2); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.4);" onclick="handleResolvePost('${escapeHtml(item.id)}')" title="ปรับเป็นส่งคืนแล้ว">
                                        ✅
                                    </button>
                                ` : ''}
                                <button class="btn-admin-action btn-admin-del" onclick="handleDeletePost('${escapeHtml(item.id)}')" title="ลบโพสต์นี้ออกจากระบบ">
                                    🗑️
                                </button>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>

            <!-- User Management List -->
            <div class="admin-section">
                <div class="admin-section-title">
                    <span>👥 รายชื่อนักศึกษาในระบบ (${allUsers.length})</span>
                </div>
                <div class="admin-users-list" style="max-height: 250px; overflow-y: auto;">
                    ${allUsers.map(u => `
                        <div class="admin-user-card">
                            <div class="admin-user-info">
                                <div class="admin-user-name">
                                    ${escapeHtml(u.fullName)}
                                    ${isUserAdmin(u) ? '<span class="admin-badge-gold" style="font-size:0.55rem; padding:1px 4px;">ADMIN</span>' : ''}
                                </div>
                                <div class="admin-user-sub">
                                    รหัส: <code>${escapeHtml(u.studentId)}</code> • ${escapeHtml(u.faculty || '')}
                                </div>
                                <div class="admin-user-sub" style="color: #e1306c;">
                                    IG: ${escapeHtml(u.instagram || '@-')}
                                </div>
                            </div>
                            <div class="admin-btn-group">
                                ${!isUserAdmin(u) ? `
                                    <button class="btn-admin-action btn-admin-reset" onclick="handleAdminResetPassword('${escapeHtml(u.studentId)}')" title="รีเซ็ตรหัสผ่านเป็น 123456">
                                        🔑 รีเซ็ต
                                    </button>
                                    <button class="btn-admin-action btn-admin-del" onclick="handleAdminDeleteUser('${escapeHtml(u.studentId)}')" title="ลบบัญชีผู้ใช้นี้">
                                        🗑️
                                    </button>
                                ` : '<span style="font-size:0.65rem; color:#f59e0b; font-weight:700;">Super Admin</span>'}
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>

            <!-- Maintenance Actions -->
            <div class="admin-section">
                <div class="admin-section-title">🧹 เครื่องมือบำรุงรักษาระบบ (Maintenance)</div>
                <button class="btn-portal-submit" style="background: rgba(239, 68, 68, 0.15); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.3); font-size: 0.72rem; padding: 8px;" onclick="handleAdminClearResolved()">
                    🗑️ ล้างประกาศที่ส่งคืนแล้วทั้งหมด (${resolvedCount} รายการ)
                </button>
            </div>
        </div>
    `;
}

async function handleAdminBroadcast() {
    const titleEl = document.getElementById("adminBroadcastTitle");
    const msgEl = document.getElementById("adminBroadcastMsg");
    if (!msgEl) return;
    const title = (titleEl && titleEl.value.trim()) || "ประกาศจากผู้ดูแลระบบ";
    const message = msgEl.value.trim();
    if (!message) {
        alert("กรุณากรอกข้อความประกาศ");
        return;
    }
    const res = await sendAppAction("ADMIN_BROADCAST", { title, message });
    if (res.success) {
        alert("🎉 ส่งประกาศแจ้งเตือนด่วนขึ้นระบบเรียบร้อยแล้ว!");
        if (msgEl) msgEl.value = "";
        if (titleEl) titleEl.value = "";
    } else {
        alert(res.message || "เกิดข้อผิดพลาด");
    }
    fetchPhoneState();
}

async function handleAdminDeleteBroadcast(broadcastId) {
    if (!confirm("คุณต้องการลบประกาศด่วนนี้ออกจากระบบใช่หรือไม่?")) return;
    
    // Instant optimistic update
    activeBroadcasts = activeBroadcasts.filter(b => b.id !== broadcastId);
    saveActiveBroadcasts();
    if (firestoreDb && broadcastId) {
        firestoreDb.collection("broadcasts").doc(broadcastId).delete().catch(e => console.warn(e));
        firestoreDb.collection("broadcasts").where("id", "==", broadcastId).get().then(s => s.forEach(d => d.ref.delete())).catch(e => {});
    }
    
    lastRenderedAppKey = "";
    if (clientSimState) {
        renderPhoneUI(clientSimState);
    } else if (currentPhoneState) {
        renderPhoneUI(currentPhoneState);
    }
    
    const res = await sendAppAction("ADMIN_DELETE_BROADCAST", { broadcastId });
    alert(res.message || "ลบประกาศด่วนเรียบร้อยแล้ว");
    lastRenderedAppKey = "";
    fetchPhoneState();
}

async function handleAdminResetPassword(targetId) {
    if (!confirm(`คุณต้องการรีเซ็ตรหัสผ่านของนักศึกษารหัส ${targetId} เป็น '123456' ใช่หรือไม่?`)) return;
    const res = await sendAppAction("ADMIN_RESET_PASSWORD", { studentId: targetId, newPassword: "123456" });
    alert(res.message || "ดำเนินการเรียบร้อยแล้ว");
    lastRenderedAppKey = "";
    fetchPhoneState();
}

async function handleAdminDeleteUser(targetId) {
    if (!confirm(`⚠️ ยืนยันการลบบัญชีนักศึกษารหัส ${targetId} หรือไม่? ข้อมูลจะไม่สามารถกู้คืนได้`)) return;
    const res = await sendAppAction("ADMIN_DELETE_USER", { studentId: targetId });
    alert(res.message || "ดำเนินการเรียบร้อยแล้ว");
    lastRenderedAppKey = "";
    fetchPhoneState();
}

async function handleAdminPinPost(itemId) {
    const res = await sendAppAction("ADMIN_PIN_POST", { itemId });
    if (res.success) {
        alert("📌 ปักหมุดประกาศขึ้นบนสุดของฟีดเรียบร้อยแล้ว!");
        psruUserTab = 'FEED';
        lastRenderedAppKey = "";
        fetchPhoneState();
    } else {
        alert(res.message || "ไม่สามารถปักหมุดได้");
    }
}

async function handleAdminClearResolved() {
    if (!confirm("ต้องการลบประกาศที่ส่งคืนแล้วทั้งหมดออกจากระบบใช่หรือไม่?")) return;
    const res = await sendAppAction("ADMIN_CLEAR_RESOLVED", {});
    alert(res.message || "ดำเนินการเรียบร้อยแล้ว");
    lastRenderedAppKey = "";
    fetchPhoneState();
}
