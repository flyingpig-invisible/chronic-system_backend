package com.gzu.chronic.controller;

import com.gzu.chronic.common.BizException;
import com.gzu.chronic.common.P;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import com.gzu.chronic.common.Views;
import com.gzu.chronic.security.TokenUtil;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final Repo repo;

    public AuthController(Repo repo) {
        this.repo = repo;
    }

    @PostMapping("/login")
    public R login(@RequestBody Map<String, Object> body) {
        String phone = P.s(body, "phone");
        String password = P.s(body, "password");
        if (phone == null || phone.isBlank()) {
            throw new BizException(1001, "手机号必填");
        }
        Map<String, Object> acc = repo.one("select * from t_account where phone=?", phone);
        if (acc == null || password == null || !TokenUtil.sha256(password).equals(P.s(acc, "password"))) {
            throw new BizException(1001, "手机号或密码错误");
        }
        String uid = P.s(acc, "patientId");
        String role = P.s(body, "role");
        if (role == null || role.isBlank()) {
            role = P.s(acc, "role");
        }
        return R.ok(Repo.of(
                "token", TokenUtil.create(uid, 7200),
                "userId", uid,
                "role", role,
                "roles", Repo.jsonOf(acc, "roles"),
                "expiresIn", 7200));
    }

    @PostMapping("/register")
    public R register(@RequestBody Map<String, Object> body) {
        String phone = P.s(body, "phone");
        String password = P.s(body, "password");
        if (phone == null || phone.isBlank() || password == null || password.isBlank()) {
            throw new BizException(1001, "手机号和密码必填");
        }
        if (repo.one("select phone from t_account where phone=?", phone) != null) {
            throw new BizException(2001, "该手机号已注册");
        }
        String uid = "p" + System.currentTimeMillis();
        String name = P.s(body, "realName") == null ? "新用户" : P.s(body, "realName");
        repo.update("insert into t_account (phone,password,patient_id,role,roles,real_name,created_at) values (?,?,?,?,?,?,NOW())",
                phone, TokenUtil.sha256(password), uid, "patient", "[\"patient\"]", name);
        repo.update("insert into t_patient (id,name,alias,gender,age,phone,diseases) values (?,?,?,?,?,?,?)",
                uid, name, name, "未知", 0, phone, "[]");
        return R.ok(Repo.of("userId", uid, "token", TokenUtil.create(uid, 7200)));
    }

    @PostMapping("/sms/code")
    public R smsCode(@RequestBody(required = false) Map<String, Object> body) {
        return R.ok(Repo.of("sent", true, "devCode", "123456", "expiresIn", 300));
    }

    @PostMapping("/password/forgot")
    public R forgot(@RequestBody Map<String, Object> body) {
        String phone = P.s(body, "phone");
        String np = P.s(body, "newPassword");
        if (np == null || np.isBlank()) {
            throw new BizException(1001, "新密码必填");
        }
        int n = repo.update("update t_account set password=? where phone=?", TokenUtil.sha256(np), phone);
        if (n == 0) {
            throw new BizException(1001, "手机号未注册");
        }
        return R.ok(Repo.of("ok", true));
    }

    @GetMapping("/roles")
    public R roles() {
        List<Map<String, Object>> rows = repo.list(
                "select role_key,name,icon,tone,description,tag from t_role order by role_key");
        rows.replaceAll(Views::role);
        return R.ok(rows);
    }

    @PostMapping("/role/switch")
    public R switchRole(@RequestBody Map<String, Object> body, @RequestAttribute("uid") String uid) {
        String role = P.s(body, "role");
        Map<String, Object> acc = repo.one("select * from t_account where patient_id=?", uid);
        Object roles = acc == null ? null : Repo.jsonOf(acc, "roles");
        if (roles instanceof List<?> l && role != null && !l.contains(role)) {
            throw new BizException(403, "当前账号没有该角色权限");
        }
        return R.ok(Repo.of("token", TokenUtil.create(uid, 7200), "role", role));
    }
}
