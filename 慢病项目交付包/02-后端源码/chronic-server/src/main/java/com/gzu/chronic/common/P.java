package com.gzu.chronic.common;

import java.util.List;
import java.util.Map;

/** 参数取值与字段改名小工具（DB 列名 -> 契约字段名） */
public final class P {

    private P() {
    }

    public static String s(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    public static String s(Map<String, Object> m, String k) {
        return m == null ? null : s(m.get(k));
    }

    public static int i(Object o, int def) {
        try {
            return o == null ? def : Integer.parseInt(String.valueOf(o).trim());
        } catch (Exception e) {
            return def;
        }
    }

    public static int i(Map<String, Object> m, String k, int def) {
        return i(m == null ? null : m.get(k), def);
    }

    public static void rename(Map<String, Object> m, String from, String to) {
        if (m == null || from.equals(to)) {
            return;
        }
        if (m.containsKey(from)) {
            m.put(to, m.remove(from));
        }
    }

    public static void renameAll(List<Map<String, Object>> rows, String from, String to) {
        for (Map<String, Object> r : rows) {
            rename(r, from, to);
        }
    }

    /** 把 JSON 列解析后原地替换 */
    public static void json(Map<String, Object> m, String key) {
        if (m == null) {
            return;
        }
        Object v = Repo.jsonOf(m, key);
        m.put(key, v == null ? List.of() : v);
    }
}
