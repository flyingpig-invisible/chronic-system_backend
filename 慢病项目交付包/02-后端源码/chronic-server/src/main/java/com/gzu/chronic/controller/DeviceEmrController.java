package com.gzu.chronic.controller;

import com.gzu.chronic.common.BizException;
import com.gzu.chronic.common.Json;
import com.gzu.chronic.common.P;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import com.gzu.chronic.common.Views;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceEmrController {

    private final Repo repo;

    public DeviceEmrController(Repo repo) {
        this.repo = repo;
    }

    /* ---------------- 设备 ---------------- */

    @GetMapping("/device/list")
    public R devices(@RequestAttribute("uid") String uid) {
        List<Map<String, Object>> rows = repo.list("select * from t_device where patient_id=? order by id", uid);
        rows.replaceAll(Views::device);
        return R.ok(rows);
    }

    @PostMapping("/device/bind")
    public R bind(@RequestBody Map<String, Object> body, @RequestAttribute("uid") String uid) {
        String sn = P.s(body, "sn");
        if (sn == null || sn.isBlank()) {
            throw new BizException(1001, "设备序列号 sn 必填");
        }
        String id = "d" + System.currentTimeMillis();
        repo.update("insert into t_device (id,patient_id,name,model,online,battery,sync_at,types) values (?,?,?,?,?,?,?,?)",
                id, uid,
                P.s(body, "name") == null ? "新设备" : P.s(body, "name"),
                P.s(body, "model") == null ? sn : P.s(body, "model"),
                Boolean.TRUE, 100, "刚刚", Json.to(List.of("心率", "血氧")));
        return R.ok(Repo.of("id", id));
    }

    @DeleteMapping("/device/{id}")
    public R unbind(@PathVariable("id") String id, @RequestAttribute("uid") String uid) {
        int n = repo.update("delete from t_device where id=? and patient_id=?", id, uid);
        if (n == 0) {
            throw new BizException(2001, "该设备不存在");
        }
        return R.ok(Repo.of("ok", true));
    }

    /* ---------------- 电子病历 ---------------- */

    @GetMapping("/emr/list")
    public R emrList(@RequestAttribute("uid") String uid) {
        List<Map<String, Object>> rows = repo.list("select * from t_emr where patient_id=?", uid);
        for (Map<String, Object> r : rows) {
            Object payload = Repo.jsonOf(r, "payload");
            if (payload instanceof Map<?, ?> m) {
                m.forEach((k, v) -> r.put(String.valueOf(k), v));
            }
            r.remove("patientId");
        }
        return R.ok(Repo.page(rows, rows.size()));
    }

    @PostMapping("/emr/upload")
    public R upload(@RequestBody Map<String, Object> body, @RequestAttribute("uid") String uid) {
        String url = P.s(body, "url");
        if (url == null || url.isBlank()) {
            throw new BizException(1001, "请先用 /common/upload 上传文件，再提交 url");
        }
        String id = "e" + System.currentTimeMillis();
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("id", id);
        payload.put("url", url);
        payload.put("type", P.s(body, "type"));
        payload.put("hospital", P.s(body, "hospital"));
        payload.put("visitDate", P.s(body, "visitDate"));
        payload.put("createdAt", java.time.LocalDateTime.now().toString().replace('T', ' ').substring(0, 19));
        repo.update("insert into t_emr (id,patient_id,archive_no,payload) values (?,?,?,?)",
                id, uid, "AR" + System.currentTimeMillis(), Json.to(payload));
        return R.ok(Repo.of("id", id));
    }
}
