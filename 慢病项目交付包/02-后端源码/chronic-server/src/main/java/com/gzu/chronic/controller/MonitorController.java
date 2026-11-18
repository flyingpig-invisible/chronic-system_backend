package com.gzu.chronic.controller;

import com.gzu.chronic.common.BizException;
import com.gzu.chronic.common.P;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import com.gzu.chronic.common.Views;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/monitor")
public class MonitorController {

    private final Repo repo;

    public MonitorController(Repo repo) {
        this.repo = repo;
    }

    /** 趋势：直接取该病种预存的趋势 JSON（labels/series/stats/target 全在里面） */
    @GetMapping("/trends")
    public R trends(@RequestParam(name = "type", defaultValue = "bp") String type,
                    @RequestParam(name = "range", defaultValue = "week") String range) {
        Map<String, Object> row = repo.one("select payload from t_trend where trend_type=?", type);
        if (row == null) {
            throw new BizException(1001, "不支持的监测类型：" + type);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("range", range);
        Object payload = Repo.jsonOf(row, "payload");
        if (payload instanceof Map<?, ?> m) {
            m.forEach((k, v) -> data.put(String.valueOf(k), v));
        }
        return R.ok(data);
    }

    @GetMapping("/records")
    public R records(@RequestParam(name = "type", required = false) String type,
                     @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
                     @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), 50);
        int offset = (Math.max(pageNum, 1) - 1) * size;

        List<Map<String, Object>> rows;
        long total;
        if (type == null || type.isBlank()) {
            total = repo.count("select count(*) from t_record");
            rows = repo.list("select * from t_record order by measured_at desc limit ? offset ?", size, offset);
        } else {
            total = repo.count("select count(*) from t_record where record_type=?", type);
            rows = repo.list("select * from t_record where record_type=? order by measured_at desc limit ? offset ?",
                    type, size, offset);
        }
        rows.replaceAll(Views::record);
        return R.ok(Repo.page(rows, total));
    }

    @PostMapping("/records")
    public R addRecord(@RequestBody Map<String, Object> body, @RequestAttribute("uid") String uid) {
        String type = P.s(body, "type");
        String value = P.s(body, "value");
        if (type == null || value == null) {
            throw new BizException(1001, "type 与 value 必填");
        }
        String id = "r" + System.currentTimeMillis();
        repo.update("insert into t_record (id,patient_id,record_type,record_value,unit,time_text,status,label,color,measured_at)"
                        + " values (?,?,?,?,?,?,?,?,?,NOW())",
                id, uid, type, value,
                P.s(body, "unit") == null ? "" : P.s(body, "unit"),
                "刚刚", "normal", "正常", "green");
        return R.ok(Repo.of("id", id));
    }

    @GetMapping("/disease-tabs")
    public R diseaseTabs() {
        List<Map<String, Object>> rows = repo.list("select * from t_disease_tab order by sort_no");
        rows.replaceAll(Views::tab);
        return R.ok(rows);
    }
}
