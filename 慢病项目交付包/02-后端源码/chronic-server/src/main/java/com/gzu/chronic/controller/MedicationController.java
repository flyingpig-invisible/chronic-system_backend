package com.gzu.chronic.controller;

import com.gzu.chronic.common.BizException;
import com.gzu.chronic.common.P;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import com.gzu.chronic.common.Views;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/medication")
public class MedicationController {

    private final Repo repo;

    public MedicationController(Repo repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public R list(@RequestAttribute("uid") String uid) {
        List<Map<String, Object>> rows = repo.list("select * from t_medication where patient_id=? order by id", uid);
        rows.replaceAll(Views::medication);
        return R.ok(rows);
    }

    @PostMapping("/checkin")
    public R checkin(@RequestBody Map<String, Object> body, @RequestAttribute("uid") String uid) {
        String mid = P.s(body, "medicationId");
        if (mid == null || mid.isBlank()) {
            throw new BizException(1001, "medicationId 必填");
        }
        Map<String, Object> med = repo.one("select * from t_medication where id=? and patient_id=?", mid, uid);
        if (med == null) {
            throw new BizException(2001, "该用药计划不存在");
        }
        if ("taken".equals(P.s(med, "status"))) {
            throw new BizException(2001, "今天已经打卡过了");
        }
        String at = P.s(body, "time") == null ? "刚刚" : "今天 " + P.s(body, "time");
        repo.update("update t_medication set status='taken', taken_at=? where id=?", at, mid);
        return R.ok(Repo.of("ok", true, "takenAt", at));
    }
}
