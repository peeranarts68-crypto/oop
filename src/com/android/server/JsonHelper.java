package com.android.server;

import java.util.Collection;
import java.util.Map;

/**
 * ============================================================================
 * [Lecture 5.2: MVC Architectural Pattern - Data Serialization Helper]
 * ============================================================================
 * 
 * คลาสเครื่องมือช่วยแปลง Java Objects ให้อยู่ในรูปแบบสตริง JSON (JSON Serializer)
 * ออกแบบให้ทำงานได้ด้วย Pure Java โดยไม่ต้องพึ่งพาไลบรารีภายนอก (Zero External Dependencies)
 * 
 * รองรับการแปลง:
 * 1. ข้อมูลพื้นฐาน (String, Number, Boolean, null)
 * 2. โครงสร้างข้อมูล Map<K, V> -> JSON Object {...}
 * 3. โครงสร้างข้อมูล Collection / List<T> -> JSON Array [...]
 * 4. การจัดการ Escape Characters สำหรับตัวอักษรพิเศษและ Unicode ภาษาไทย
 * 
 * @author CPE Student
 * @version 1.0
 */
public class JsonHelper {

    /**
     * แปลงอ็อบเจกต์ Java ใดๆ ให้กลายเป็นสตริง JSON
     * 
     * @param obj อ็อบเจกต์ที่ต้องการแปลง เช่น Map, List, String, Number
     * @return สตริงรูปแบบ JSON
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Collection<?> col) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }

        // กรณีเป็น Object อื่นๆ ให้เรียก toString() แล้ว escape สตริง
        return "\"" + escape(obj.toString()) + "\"";
    }

    /**
     * แปลงตัวอักษรพิเศษให้เป็น Escape Sequence ตามมาตรฐาน JSON
     * เช่น เครื่องหมายคำพูด ("), backslash (\), ขึ้นบรรทัดใหม่ (\n), แท็บ (\t)
     * รวมทั้งแปลงอักขระพิเศษให้อยู่ในรูป Unicode Escape (รหัสเลขฐานสิบหก 4 หลัก)
     * 
     * @param s ข้อความต้นทาง
     * @return ข้อความที่ผ่านการ Escape แล้ว
     */
    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ' || c > 127) {
                        String hex = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(hex.substring(hex.length() - 4));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
