package com.gzu.chronic.controller;

import com.gzu.chronic.common.P;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import com.gzu.chronic.common.Views;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final Repo repo;

    public HomeController(Repo repo) {
        this.repo = repo;
    }

    /** 首页聚合：一次返回 patient / risk / todayMetrics / todayTasks / quickActions */
    @GetMapping("/home/summary")
    public R summary(@RequestAttribute("uid") String uid) {
        Map<String, Object> patient = Views.patient(repo.one("select * from t_patient where id=?", uid));
        Map<String, Object> risk = repo.one(
                "select level,tone,summary,advice,score,checkin_days,rank_text from t_risk where patient_id=?", uid);
        if (risk != null) {
            P.rename(risk, "tone", "type");
            P.rename(risk, "rankText", "rank");
        }

        List<Map<String, Object>> metrics = repo.list(
                "select * from t_metric_today where patient_id=? order by sort_no", uid);
        metrics.replaceAll(Views::metric);

        List<Map<String, Object>> tasks = repo.list(
                "select * from t_task where patient_id=? order by sort_no", uid);
        tasks.replaceAll(Views::task);

        List<Map<String, Object>> quickActions = repo.list("select * from t_quick_action order by sort_no");

        return R.ok(Repo.of(
                "patient", patient,
                "risk", risk,
                "todayMetrics", metrics,
                "todayTasks", tasks,
                "quickActions", quickActions));
    }
}
