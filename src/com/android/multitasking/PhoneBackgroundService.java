package com.android.multitasking;

import com.android.model.AndroidPhone;

/**
 * ============================================================================
 * [Lecture 6: Multitasking - Threads & Background Services]
 * ============================================================================
 * 
 * คลาสจำลองการทำงานพร้อมกันหลายงาน (Multitasking) ของระบบปฏิบัติการ Android
 * 
 * หลักการทำงานตามเนื้อหา Lecture 6:
 * 1. implements อินเทอร์เฟซ `Runnable` เพื่อให้สามารถรันบน Thread แยกต่างหากได้
 * 2. ทำงานเป็น Background Thread โดยไม่หน่วงหรือบล็อกการทำงานของ Main UI Thread
 * 3. ใช้คีย์เวิร์ด `volatile` สำหรับตัวแปร isRunning เพื่อรับประกันการมองเห็นค่าข้าม Thread (Thread Visibility)
 * 4. จัดการข้อยกเว้น `InterruptedException` และตั้งค่าการขัดจังหวะของ Thread อย่างถูกต้อง
 * 
 * @author CPE Student
 * @version 1.0
 */
public class PhoneBackgroundService implements Runnable {

    /** อ้างอิงสมาร์ตโฟนที่รันเซอร์วิส */
    private final AndroidPhone phone;

    /** 
     * ตัวแปรสถานะการทำงานของ Thread
     * ประกาศเป็น `volatile` เพื่อให้ทุก CPU Core มองเห็นค่าล่าสุดเสมอโดยไม่ต้องล็อก
     */
    private volatile boolean isRunning;

    /** ตัวนับรอบเวลาการทำงานของเซอร์วิส (Tick Count) */
    private long tickCount;

    /**
     * [Lecture 3: Constructor]
     * ตัวสร้าง PhoneBackgroundService
     * @param phone โมเดลสมาร์ตโฟน
     */
    public PhoneBackgroundService(AndroidPhone phone) {
        this.phone = phone;
        this.isRunning = true;
        this.tickCount = 0;
    }

    /**
     * [Lecture 6: Thread Execution]
     * เมธอด run() ที่จะถูกเรียกทำงานเมื่อเธรดเริ่มต้น (thread.start())
     * ทำหน้าที่เป็นวงรอบการทำงานเบื้องหลัง (Background Loop) ทุกๆ 1 วินาที
     */
    @Override
    public void run() {
        System.out.println("[MULTITASKING] Background Service Thread started running...");
        while (isRunning) {
            try {
                // หน่วงเวลา 1 วินาที เพื่อจำลองการตรวจสอบระบบเป็นรอบๆ
                Thread.sleep(1000);
                tickCount++;

                // ทุกๆ 30 วินาที แสดงข้อความว่า Thread ยังคงทำงานอยู่ในเบื้องหลัง
                if (tickCount % 30 == 0) {
                    System.out.println("[MULTITASKING] Phone Background Thread active: uptime tick = " + tickCount + "s");
                }
            } catch (InterruptedException e) {
                System.out.println("[MULTITASKING] Background Thread interrupted.");
                // คืนสถานะการขัดจังหวะให้กับ Thread ปัจจุบันตาม Best Practice ของ Java Multithreading
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[MULTITASKING] Background Service Thread stopped.");
    }

    /**
     * สั่งหยุดการทำงานของ Background Service
     * โดยการปรับแฟล็ก isRunning เป็น false ซึ่งจะทำให้ลูป while หลุดออกอย่างปลอดภัย
     */
    public void stopService() {
        this.isRunning = false;
    }

    /**
     * อ่านจำนวนวินาที (Tick) ที่เซอร์วิสนี้ทำงานมาทั้งหมด
     * @return จำนวน Tick วินาที
     */
    public long getTickCount() {
        return tickCount;
    }
}
