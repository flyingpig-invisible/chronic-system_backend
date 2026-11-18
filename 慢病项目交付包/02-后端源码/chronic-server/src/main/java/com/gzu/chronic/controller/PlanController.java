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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plan")
public class PlanController {

    private final Repo repo;

    public PlanController(Repo repo) {
        this.repo = repo;
    }

    @GetMapping("/today")
    public R today(@RequestAttribute("uid") String uid) {
        List<Map<String, Object>> tasks = repo.list(
                "select * from t_task where patient_id=? order by sort_no", uid);
        tasks.replaceAll(Views::task);

        List<Map<String, Object>> plans = repo.list(
                "select * from t_doctor_plan where patient_id=? order by id", uid);
        plans.replaceAll(Views::plan);

        return R.ok(Repo.of("tasks", tasks, "doctorPlans", plans));
    }

    @PutMapping("/task/{id}/status")
    public R updateStatus(@PathVariable("id") String id,
                          @RequestBody Map<String, Object> body,
                          @RequestAttribute("uid") String uid) {
        String status = P.s(body, "status");
        if (status == null || status.isBlank()) {
            throw new BizException(1001, "status 必填（done / skip）");
        }
        boolean done = "done".equalsIgnoreCase(status);
        int n = repo.update("update t_task set done=? where id=? and patient_id=?", done, id, uid);
        if (n == 0) {
            throw new BizException(2001, "该任务不存在");
        }
        return R.ok(Repo.of("ok", true, "done", done));
    }
}
