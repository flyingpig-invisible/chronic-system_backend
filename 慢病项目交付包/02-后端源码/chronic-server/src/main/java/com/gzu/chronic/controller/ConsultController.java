package com.gzu.chronic.controller;

import com.gzu.chronic.common.BizException;
import com.gzu.chronic.common.Json;
import com.gzu.chronic.common.P;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import com.gzu.chronic.common.Views;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsultController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Repo repo;

    public ConsultController(Repo repo) {
        this.repo = repo;
    }

    @PostMapping("/chat/send")
    public R send(@RequestBody Map<String, Object> body, @RequestAttribute("uid") String uid) {
        String sessionId = P.s(body, "sessionId") == null ? "s001" : P.s(body, "sessionId");
        String question = P.s(body, "question");
        if (question == null || question.isBlank()) {
            throw new BizException(1001, "问题不能为空");
        }
        String now = LocalDateTime.now().format(TS);
        repo.update("insert into t_chat_message (id,session_id,patient_id,role,msg_type,content,citations,created_at)"
                        + " values (?,?,?,?,?,?,?,?)",
                "c" + System.nanoTime(), sessionId, uid, "user", "text", question, "[]", now);

        String answer = answer(question);
        List<Map<String, Object>> citations = List.of(
                citation("中国高血压防治指南（2023 年修订版）", "指南"),
                citation("中国 2 型糖尿病防治指南（2023 年版）", "指南"));
        String safetyTip = "以上内容仅供参考，不能替代医生面诊；出现胸痛、意识改变、血压≥180/110 等情况请立即就医或拨打 120。";

        repo.update("insert into t_chat_message (id,session_id,patient_id,role,msg_type,content,citations,created_at)"
                        + " values (?,?,?,?,?,?,?,?)",
                "c" + System.nanoTime(), sessionId, uid, "assistant", "text", answer, Json.to(citations),
                LocalDateTime.now().format(TS));

        return R.ok(Repo.of(
                "sessionId", sessionId,
                "answer", answer,
                "citations", citations,
                "safetyTip", safetyTip));
    }

    /** 规则应答（关键词命中）；接大模型时只替换这一个方法 */
    private String answer(String q) {
        if (q.contains("血压")) {
            Map<String, Object> bp = repo.one("select * from t_trend where trend_type='bp'");
            String latest = "132/84 mmHg";
            if (bp != null) {
                Object payload = Repo.jsonOf(bp, "payload");
                if (payload instanceof Map<?, ?> m && m.get("series") instanceof List<?> series && !series.isEmpty()
                        && series.get(0) instanceof Map<?, ?> s0 && s0.get("data") instanceof List<?> data && !data.isEmpty()) {
                    latest = String.valueOf(data.get(data.size() - 1)) + " mmHg（最近一次收缩压）";
                }
            }
            return "你的最近一次血压记录为 " + latest + "。建议每天早晚各测一次，测前静坐 5 分钟；"
                    + "若连续 3 天高于 140/90，请把记录带给医生调整方案。";
        }
        if (q.contains("血糖")) {
            return "空腹血糖建议控制在 4.4–7.0 mmol/L，餐后 2 小时低于 10.0 mmol/L。"
                    + "主食减半、先菜后饭、餐后散步 15 分钟，对餐后血糖最有效。";
        }
        if (q.contains("头晕") || q.contains("头痛")) {
            return "头晕需要先排除血压过高或过低：请立刻测一次血压并记录。"
                    + "若伴随言语不清、单侧肢体无力、视物重影，请立即拨打 120。";
        }
        if (q.contains("药") || q.contains("吃") && q.contains("怎么")) {
            return "降压药与降糖药都要固定时间服用，不要因为当天血压正常就自行停药。"
                    + "漏服一次不要下次加倍，按原计划继续即可，并联系医生说明。";
        }
        if (q.contains("吃") || q.contains("饮食") || q.contains("食谱")) {
            return "每日食盐控制在 5 克以内（约一啤酒瓶盖），少吃腌制品与加工肉；"
                    + "优先选择杂粮、深色蔬菜、鱼类，油每日不超过 25 克。";
        }
        return "已记录你的问题。建议先补充一项客观数据（血压/血糖/用药情况），"
                + "我会结合你的近期记录给出更具体的建议；持续不适请及时就医。";
    }

    private static Map<String, Object> citation(String title, String source) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("source", source);
        m.put("url", "");
        return m;
    }

    @GetMapping("/chat/history")
    public R history(@RequestParam(name = "sessionId", defaultValue = "s001") String sessionId,
                     @RequestAttribute("uid") String uid) {
        List<Map<String, Object>> rows = repo.list(
                "select * from t_chat_message where session_id=? and patient_id=? order by created_at, id",
                sessionId, uid);
        rows.replaceAll(Views::chat);
        return R.ok(rows);
    }

    @GetMapping("/chat/quick-questions")
    public R quickQuestions() {
        return R.ok(repo.list("select question from t_quick_question order by sort_no"));
    }

    /* ---------------- 远程医疗 ---------------- */

    @GetMapping("/telemedicine/doctors")
    public R telemedicine(@RequestParam(name = "dept", required = false) String dept) {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> rm = repo.one("select payload from t_remote_medical where id='rm1'");
        if (rm != null) {
            Object payload = Repo.jsonOf(rm, "payload");
            if (payload instanceof Map<?, ?> m) {
                m.forEach((k, v) -> data.put(String.valueOf(k), v));
            }
        }
        List<Map<String, Object>> doctors = (dept == null || dept.isBlank())
                ? repo.list("select * from t_doctor order by id")
                : repo.list("select * from t_doctor where dept like ? order by id", "%" + dept + "%");
        data.put("doctors", doctors);
        return R.ok(data);
    }

    @PostMapping("/telemedicine/appointment")
    public R appointment(@RequestBody Map<String, Object> body, @RequestAttribute("uid") String uid) {
        String doctorId = P.s(body, "doctorId");
        if (doctorId == null || doctorId.isBlank()) {
            throw new BizException(1001, "doctorId 必填");
        }
        if (repo.one("select id from t_doctor where id=?", doctorId) == null) {
            throw new BizException(2001, "医生不存在");
        }
        String id = "ap" + System.currentTimeMillis();
        repo.update("insert into t_appointment (id,patient_id,doctor_id,slot,symptom,status,created_at)"
                        + " values (?,?,?,?,?,?,?)",
                id, uid, doctorId, P.s(body, "slot"), P.s(body, "symptom"), "pending",
                LocalDateTime.now().format(TS));
        return R.ok(Repo.of("ok", true, "orderNo", id));
    }
}
