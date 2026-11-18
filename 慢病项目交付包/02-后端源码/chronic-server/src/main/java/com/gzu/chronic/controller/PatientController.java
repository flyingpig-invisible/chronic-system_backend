package com.gzu.chronic.controller;

import com.gzu.chronic.common.BizException;
import com.gzu.chronic.common.Json;
import com.gzu.chronic.common.P;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import com.gzu.chronic.common.Views;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private static final String[][] EDITABLE = {
            {"name", "name"}, {"alias", "alias"}, {"gender", "gender"}, {"age", "age"},
            {"height", "height"}, {"weight", "weight"}, {"bmi", "bmi"}, {"phone", "phone"},
            {"insurance", "insurance"}, {"home", "home"}, {"idcard", "id_card"}
    };

    private final Repo repo;

    public PatientController(Repo repo) {
        this.repo = repo;
    }

    @GetMapping("/profile")
    public R profile(@RequestAttribute("uid") String uid) {
        return R.ok(Views.patient(repo.one("select * from t_patient where id=?", uid)));
    }

    @PutMapping("/profile")
    public R update(@RequestBody Map<String, Object> body, @RequestAttribute("uid") String uid) {
        StringBuilder sb = new StringBuilder("update t_patient set ");
        List<Object> args = new ArrayList<>();
        boolean first = true;

        for (String[] c : EDITABLE) {
            if (body.containsKey(c[0])) {
                sb.append(first ? "" : ", ").append(c[1]).append("=?");
                args.add(body.get(c[0]));
                first = false;
            }
        }
        if (body.containsKey("diseases")) {
            sb.append(first ? "" : ", ").append("diseases=?");
            args.add(Json.to(body.get("diseases")));
            first = false;
        }
        if (!first) {
            sb.append(" where id=?");
            args.add(uid);
            repo.update(sb.toString(), args.toArray());
        }
        return R.ok(Views.patient(repo.one("select * from t_patient where id=?", uid)));
    }

    @GetMapping("/family")
    public R family(@RequestAttribute("uid") String uid) {
        return R.ok(repo.list("select * from t_family where patient_id=?", uid));
    }

    /** 健康评分明细（"我的"页用） */
    @GetMapping("/score-detail")
    public R scoreDetail(@RequestAttribute("uid") String uid) {
        Map<String, Object> row = repo.one("select payload from t_score_detail where patient_id=?", uid);
        if (row == null) {
            Map<String, Object> risk = repo.one("select score from t_risk where patient_id=?", uid);
            return R.ok(Repo.of("score", risk == null ? 0 : P.i(risk, "score", 0), "items", java.util.List.of()));
        }
        Object payload = Repo.jsonOf(row, "payload");
        return R.ok(payload == null ? Repo.of("score", 0, "items", java.util.List.of()) : payload);
    }

    @PostMapping("/family/bind")
    public R bind(@RequestBody Map<String, Object> body, @RequestAttribute("uid") String uid) {
        String phone = P.s(body, "phone");
        if (phone == null || phone.isBlank()) {
            throw new BizException(1001, "手机号必填");
        }
        String id = "fm" + System.currentTimeMillis();
        repo.update("insert into t_family (id,patient_id,name,relation,phone) values (?,?,?,?,?)",
                id, uid,
                P.s(body, "name") == null ? "亲属" : P.s(body, "name"),
                P.s(body, "relation"),
                phone);
        return R.ok(Repo.of("id", id));
    }

    @DeleteMapping("/family/{id}")
    public R unbind(@PathVariable("id") String id, @RequestAttribute("uid") String uid) {
        int n = repo.update("delete from t_family where id=? and patient_id=?", id, uid);
        if (n == 0) {
            throw new BizException(2001, "该亲属不存在");
        }
        return R.ok(Repo.of("ok", true));
    }
}
