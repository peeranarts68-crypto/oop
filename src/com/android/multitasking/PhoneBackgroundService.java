package com.android.multitasking;

import com.android.model.AndroidPhone;

/**
 * [Lecture 6: Multitasking]
 *
 * คลาสจำลองการทำงานพร้อมกันหลายงาน (Multitasking) ในระบบปฏิบัติการ Android
 * โดยการ implements Runnable และรันอยู่บน Thread แยกต่างหาก (Background Thread)
 * ทำหน้าที่ตรวจจับการทำงานเบื้องหลังของระบบโทรศัพท์
 */
public class PhoneBackgroundService implements Runnable {
    private final AndroidPhone phone;
    private volatile boolean isRunning;
    private long tickCount;

    public PhoneBackgroundService(AndroidPhone phone) {
        this.phone = phone;
        this.isRunning = true;
        this.tickCount = 0;
    }

    @Override
    public void run() {
        System.out.println("[MULTITASKING] Background Service Thread started running...");
        while (isRunning) {
            try {
                // หน่วงเวลา 1 วินาที จำลองการทำงานเป็นรอบในพื้นหลัง
                Thread.sleep(1000);
                tickCount++;

                // ทุกๆ 30 วินาที แสดงข้อความว่า Thread ทำงานอยู่
                if (tickCount % 30 == 0) {
                    System.out.println("[MULTITASKING] Phone Background Thread active: uptime tick = " + tickCount + "s");
                }
            } catch (InterruptedException e) {
                System.out.println("[MULTITASKING] Background Thread interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[MULTITASKING] Background Service Thread stopped.");
    }

    public void stopService() {
        this.isRunning = false;
    }

    public long getTickCount() {
        return tickCount;
    }
}
