package com.chronic.modules.ai.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.chronic.common.exception.ServiceException;
import com.chronic.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 大模型客户端
 *
 * @author chronic
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekClient {

    private final AiProperties aiProperties;

    /** 发送多轮对话，返回模型回复 */
    public ChatResult chat(String systemPrompt, List<Map<String, String>> messages) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new ServiceException("AI 服务未配置 API Key");
        }

        JSONArray msgArr = new JSONArray();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JSONObject sys = new JSONObject();
            sys.set("role", "system");
            sys.set("content", systemPrompt);
            msgArr.add(sys);
        }
        for (Map<String, String> m : messages) {
            JSONObject o = new JSONObject();
            o.set("role", m.get("role"));
            o.set("content", m.get("content"));
            msgArr.add(o);
        }

        JSONObject body = new JSONObject();
        body.set("model", aiProperties.getModel());
        body.set("messages", msgArr);
        body.set("temperature", aiProperties.getTemperature());
        body.set("max_tokens", aiProperties.getMaxTokens());
        body.set("stream", false);

        String url = aiProperties.getBaseUrl() + "/chat/completions";
        long start = System.currentTimeMillis();
        try (HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + aiProperties.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(aiProperties.getTimeout())
                .execute()) {

            String respBody = response.body();
            if (!response.isOk()) {
                log.error("DeepSeek 调用失败, status={}, body={}", response.getStatus(), respBody);
                throw new ServiceException("AI 服务调用失败：" + response.getStatus());
            }

            JSONObject json = JSONUtil.parseObj(respBody);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ServiceException("AI 服务返回异常");
            }
            String content = choices.getJSONObject(0).getJSONObject("message").getStr("content");

            ChatResult result = new ChatResult();
            result.setAnswer(content);
            result.setCostTime(System.currentTimeMillis() - start);
            JSONObject usage = json.getJSONObject("usage");
            if (usage != null) {
                result.setPromptTokens(usage.getInt("prompt_tokens", 0));
                result.setCompletionTokens(usage.getInt("completion_tokens", 0));
            }
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek 调用异常", e);
            throw new ServiceException("AI 服务调用异常：" + e.getMessage());
        }
    }

    /** 简单单轮对话 */
    public ChatResult chat(String systemPrompt, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> m = new LinkedHashMap<>();
        m.put("role", "user");
        m.put("content", userMessage);
        messages.add(m);
        return chat(systemPrompt, messages);
    }

    /** 调用结果 */
    public static class ChatResult {
        private String answer;
        private Long costTime;
        private Integer promptTokens = 0;
        private Integer completionTokens = 0;

        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public Long getCostTime() { return costTime; }
        public void setCostTime(Long costTime) { this.costTime = costTime; }
        public Integer getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    }
}
