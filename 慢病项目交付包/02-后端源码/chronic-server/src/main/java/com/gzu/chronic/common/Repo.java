package com.gzu.chronic.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 数据访问薄封装：
 * 1) 统一把下划线字段名转成 lowerCamelCase（前端契约要求）
 * 2) 提供 jsonOf() 把 JSON 字符串列解析成对象
 * 3) 分页返回 { total, list }
 */
@Repository
public class Repo {

    private final JdbcTemplate jdbc;

    public Repo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(String sql, Object... args) {
        List<Map<String, Object>> raw = jdbc.queryForList(sql, args);
        List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (Map<String, Object> row : raw) {
            out.add(camel(row));
        }
        return out;
    }

    public Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = list(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int update(String sql, Object... args) {
        return jdbc.update(sql, args);
    }

    public long count(String sql, Object... args) {
        Number n = jdbc.queryForObject(sql, Number.class, args);
        return n == null ? 0L : n.longValue();
    }

    public static Map<String, Object> camel(Map<String, Object> in) {
        Map<String, Object> out = new LinkedHashMap<>();
        in.forEach((k, v) -> out.put(toCamel(k), v));
        return out;
    }

    private static String toCamel(String s) {
        if (s.indexOf('_') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : s.toLowerCase().toCharArray()) {
            if (c == '_') {
                up = true;
                continue;
            }
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return sb.toString();
    }

    /** 取 JSON 列并解析（同时把原字符串字段移除） */
    public static Object jsonOf(Map<String, Object> row, String key) {
        if (row == null) {
            return null;
        }
        Object v = row.remove(key);
        return v == null ? null : Json.parse(String.valueOf(v));
    }

    public static Map<String, Object> page(List<Map<String, Object>> rows, long total) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("list", rows);
        return m;
    }

    public static Map<String, Object> of(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
}
