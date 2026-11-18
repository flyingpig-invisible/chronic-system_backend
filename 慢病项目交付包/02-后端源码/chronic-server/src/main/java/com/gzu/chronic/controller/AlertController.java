package com.gzu.chronic.controller;

import com.gzu.chronic.common.BizException;
import com.gzu.chronic.common.P;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import com.gzu.chronic.common.Views;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alert")
public class AlertController {

    private final Repo repo;

    public AlertController(Repo repo) {
        this.repo = repo;
    }

    @GetMapping("/summary")
    public R summary(@RequestAttribute("uid") String uid) {
        Map<String, Object> r = repo.one("select * from t_alert_summary where patient_id=?", uid);
        if (r == null) {
            return R.ok(Repo.of("high", 0, "mid", 0, "low", 0, "tip", "", "subTip", ""));
        }
        return R.ok(Repo.of(
                "high", P.i(r, "highCnt", 0),
                "mid", P.i(r, "midCnt", 0),
                "low", P.i(r, "lowCnt", 0),
                "tip", P.s(r, "tip"),
                "subTip", P.s(r, "subTip")));
    }

    @GetMapping("/list")
    public R list(@RequestParam(name = "level", required = false) String level,
                  @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
                  @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                  @RequestAttribute("uid") String uid) {
        int size = Math.min(Math.max(pageSize, 1), 50);
        int offset = (Math.max(pageNum, 1) - 1) * size;

        List<Map<String, Object>> rows;
        long total;
        if (level == null || level.isBlank()) {
            total = repo.count("select count(*) from t_alert where patient_id=?", uid);
            rows = repo.list("select * from t_alert where patient_id=? order by id limit ? offset ?", uid, size, offset);
        } else {
            total = repo.count("select count(*) from t_alert where patient_id=? and level=?", uid, level);
            rows = repo.list("select * from t_alert where patient_id=? and level=? order by id limit ? offset ?",
                    uid, level, size, offset);
        }
        rows.replaceAll(Views::alert);
        return R.ok(Repo.page(rows, total));
    }

    @PutMapping("/{id}/read")
    public R read(@PathVariable("id") String id, @RequestAttribute("uid") String uid) {
        int n = repo.update("update t_alert set is_read=TRUE, status='read' where id=? and patient_id=?", id, uid);
        if (n == 0) {
            throw new BizException(2001, "该预警不存在");
        }
        return R.ok(Repo.of("ok", true));
    }
}
