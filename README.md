# 📱 Android Phone OOP Architecture Simulator (Java 21)

แบบจำลองสมาร์ตโฟนระบบ **Android** ตามหลักการ **Object-Oriented Programming (OOP)** ครบทั้ง 4 เสาหลัก พร้อมเว็บแอปพลิเคชันจำลองหน้าจอมือถือแบบ Interactive สำหรับรันและทดสอบบน **`http://localhost:8088`** พัฒนาด้วยภาษา **Java 21 (Zero-Dependencies)**

---

## 🚀 วิธีการรันโปรเจกต์ (Quick Start)

### วิธีที่ 1: ดับเบิลคลิกไฟล์สคริปต์
- **Windows Batch**: ดับเบิลคลิกที่ไฟล์ [`run.bat`](file:///d:/0500/oop/run.bat) (ระบบจะตรวจหา Java SDK คอมไพล์ และเปิดบราวเซอร์ให้อัตโนมัติ)
- **PowerShell**: รันคำสั่ง `.\run.ps1`

### วิธีที่ 2: รันผ่าน Terminal / Command Line
```powershell
# คอมไพล์ซอร์สโค้ด
& "C:\Program Files\Android\Android Studio\jbr\bin\javac.exe" -encoding UTF-8 -d bin (Get-ChildItem -Path src -Filter "*.java" -Recurse | ForEach-Object { $_.FullName })

# รันเซิร์ฟเวอร์
& "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -cp bin com.android.Main
```
จากนั้นเปิดบราวเซอร์ไปที่: **`http://localhost:8088`**

---

## 🏛️ สถาปัตยกรรม OOP (Object-Oriented Architecture)

### 1. Abstraction (การกำหนดนามธรรมและส่วนต่อประสาน)
- [`Device`](file:///d:/0500/oop/src/com/android/model/Device.java) : `abstract class` กำหนดพฤติกรรมพื้นฐานของอุปกรณ์ (`powerOn()`, `powerOff()`, `getDeviceType()`)
- [`AndroidApplication`](file:///d:/0500/oop/src/com/android/model/apps/AndroidApplication.java) : `abstract class` กำหนดโครงสร้างและวงจรชีวิต (Lifecycle) ของแอป (`onCreate()`, `onStart()`, `onResume()`, `onPause()`, `onStop()`, `onDestroy()`)
- [`NetworkAdapter`](file:///d:/0500/oop/src/com/android/model/hardware/NetworkAdapter.java) : `interface` ตัดขาดการเชื่อมต่อเครือข่าย ให้สามารถสลับระหว่าง Wi-Fi และ Cellular ได้
- [`NotificationListener`](file:///d:/0500/oop/src/com/android/model/os/NotificationListener.java) : `interface` สำหรับ Observer ในการรับแจ้งเตือน

### 2. Inheritance (การสืบทอดคุณสมบัติ)
```
Device (abstract)
└── SmartPhone (abstract)
    └── AndroidPhone (concrete)

AndroidApplication (abstract)
├── DialerApp (แอปโทรศัพท์)
├── CameraApp (แอปถ่ายภาพ)
├── NotesApp (แอปบันทึกโน้ต)
├── CalculatorApp (แอปเครื่องคิดเลข)
└── SettingsApp (แอปตั้งค่าระบบ)
```

### 3. Polymorphism (การพ้องรูป)
- **Dynamic Method Dispatch**: เมธอด `executeAction(String action, Map params)` ใน `AndroidApplication` ถูก Overwrite ในแอปแต่ละตัว เพื่อทำงานตามตรรกะเฉพาะของแอปนั้น
- ตัวควบคุมใน `AndroidPhone` จัดการแอปผ่านตัวแปรประเภทแม่ (`AndroidApplication`) โดยไม่ต้องผูกติดกับคลาสลูกที่เฉพาะเจาะจง

### 4. Encapsulation & Composition (การห่อหุ้มและการประกอบส่วน)
- ฟิลด์สถานะทั้งหมดเป็น `private` เช่น `level` ใน [`Battery`](file:///d:/0500/oop/src/com/android/model/hardware/Battery.java), `usedCapacityMb` ใน [`Storage`](file:///d:/0500/oop/src/com/android/model/hardware/Storage.java), `brightness` ใน [`Display`](file:///d:/0500/oop/src/com/android/model/hardware/Display.java)
- มีการตรวจสอบเงื่อนไข (Data Validation) ทุกครั้งที่มีการใช้งาน เช่น แบตเตอรี่ไม่สามารถติดลบหรือเกิน 100%, ปริมาณความจุข้อมูลไม่สามารถบันทึกเกิน Free Space ที่มีอยู่จริง

---

## 🎨 Design Patterns ที่นำมาใช้

1. **Observer Pattern**:
   - `NotificationManager` ทำหน้าที่เป็น Subject
   - `NotificationListener` ทำหน้าที่เป็น Observer คอยกระจายการแจ้งเตือนไปยังหน้าจอและระบบ
2. **Factory Pattern**:
   - [`AppFactory.createApp(type, phone)`](file:///d:/0500/oop/src/com/android/model/apps/AppFactory.java) สร้าง Object ของแอปพลิเคชันตามชนิดที่ระบุ
3. **Activity Lifecycle (State Pattern)**:
   - ควบคุมการเปลี่ยนผ่านสถานะของแต่ละแอป เช่น `CREATED` ➔ `STARTED` ➔ `RESUMED` ➔ `PAUSED` ➔ `STOPPED` ➔ `DESTROYED`

---

## 🌐 รายการ REST API บน Localhost

| Method | Endpoint | รายละเอียด |
| :--- | :--- | :--- |
| `GET` | `/api/state` | ดึงสถานะปัจจุบันของมือถือ, แบตเตอรี่, จอภาพ, เมมโมรี่ และแอปที่กำลังเปิด |
| `POST` | `/api/power` | สลับเปิด/ปิดเครื่อง (Power On / Off) |
| `POST` | `/api/home` | กดปุ่ม Home สลับกลับมาที่หน้า Launcher |
| `POST` | `/api/back` | กดปุ่ม Back ปิดแอปปัจจุบัน |
| `POST` | `/api/volume/up` | เพิ่มระดับเสียงลำโพง |
| `POST` | `/api/volume/down` | ลดระดับเสียงลำโพง |
| `POST` | `/api/battery/charge` | ชาร์จแบตเตอรี่ (ระบุ `amount` ได้) |
| `POST` | `/api/app/launch` | เปิดใช้งานแอปตาม `appId` |
| `POST` | `/api/app/close` | ปิดแอปพลิเคชันปัจจุบัน |
| `POST` | `/api/app/action` | ส่งคำสั่งไปยังแอป เช่น `CAPTURE`, `CALL`, `ADD_NOTE`, `CALCULATE` |
| `POST` | `/api/notifications/clear` | ล้างรายการแจ้งเตือนทั้งหมด |
