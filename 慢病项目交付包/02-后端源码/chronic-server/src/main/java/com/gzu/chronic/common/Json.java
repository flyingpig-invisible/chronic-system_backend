package com.gzu.chronic.common;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 极简 JSON 工具：DB 里以 JSON 字符串存的字段，读出来时解析成对象 */
public final class Json {

    public static final ObjectMapper M = new ObjectMapper();

    private Json() {
    }

    public static Object parse(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return M.readValue(s, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static String to(Object o) {
        try {
            return M.writeValueAsString(o);
        } catch (Exception e) {
            return "null";
        }
    }
}
