package com.gzu.chronic.common;

import java.util.List;
import java.util.Map;

/** 把数据库行整形成契约里约定的字段名（前端渲染代码不用改） */
public final class Views {

    private Views() {
    }

    public static Map<String, Object> patient(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Map<String, Object> doctor = new java.util.LinkedHashMap<>();
        doctor.put("name", row.remove("doctorName"));
        doctor.put("title", row.remove("doctorTitle"));
        doctor.put("dept", row.remove("doctorDept"));
        doctor.put("hospital", row.remove("doctorHospital"));
        row.put("doctor", doctor);
        P.json(row, "diseases");
        P.rename(row, "idCard", "idcard");
        return row;
    }

    public static Map<String, Object> role(Map<String, Object> row) {
        P.rename(row, "roleKey", "key");
        return row;
    }

    public static Map<String, Object> tab(Map<String, Object> row) {
        P.rename(row, "tabKey", "key");
        return row;
    }

    public static Map<String, Object> metric(Map<String, Object> row) {
        P.rename(row, "metricKey", "key");
        P.rename(row, "metricValue", "value");
        return row;
    }

    public static Map<String, Object> task(Map<String, Object> row) {
        P.rename(row, "taskType", "type");
        P.rename(row, "description", "desc");
        return row;
    }

    public static Map<String, Object> record(Map<String, Object> row) {
        P.rename(row, "recordType", "type");
        P.rename(row, "recordValue", "value");
        P.rename(row, "timeText", "time");
        return row;
    }

    public static Map<String, Object> alert(Map<String, Object> row) {
        P.rename(row, "alertValue", "value");
        P.rename(row, "timeText", "time");
        P.rename(row, "description", "desc");
        P.rename(row, "isRead", "read");
        return row;
    }

    public static Map<String, Object> medication(Map<String, Object> row) {
        P.rename(row, "timeText", "time");
        return row;
    }

    public static Map<String, Object> plan(Map<String, Object> row) {
        P.rename(row, "description", "desc");
        return row;
    }

    public static Map<String, Object> recipe(Map<String, Object> row) {
        P.json(row, "tags");
        P.rename(row, "description", "desc");
        return row;
    }

    public static Map<String, Object> guide(Map<String, Object> row) {
        P.rename(row, "description", "desc");
        return row;
    }

    public static Map<String, Object> article(Map<String, Object> row) {
        P.rename(row, "timeText", "time");
        return row;
    }

    public static Map<String, Object> device(Map<String, Object> row) {
        P.json(row, "types");
        return row;
    }

    public static Map<String, Object> chat(Map<String, Object> row) {
        P.rename(row, "msgType", "type");
        P.rename(row, "content", "text");
        P.json(row, "citations");
        P.rename(row, "createdAt", "time");
        return row;
    }

}
