package com.chronic.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.chronic.common.exception.ServiceException;
import com.chronic.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 微信 code2Session 助手
 * <p>
 * 只有配置了 app-id / app-secret 时才会真正请求微信服务器；
 * 未配置（开发环境）时返回 null，由调用方走手机号+密码登录。
 *
 * @author chronic
 */
@Slf4j
@Component
public class WxAuthHelper {

    @Value("${wx.miniapp.app-id:}")
    private String appId;

    @Value("${wx.miniapp.app-secret:}")
    private String appSecret;

    /** 是否已配置微信小程序凭据 */
    public boolean enabled() {
        return StringUtils.hasText(appId) && StringUtils.hasText(appSecret);
    }

    /**
     * 用 wx.login 的 code 换取用户身份
     *
     * @return {openid, unionid}；未配置凭据或换取失败时返回 null
     */
    public JSONObject code2Session(String code) {
        if (!enabled() || !StringUtils.hasText(code)) {
            return null;
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appId
                + "&secret=" + appSecret
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        try (HttpResponse response = HttpRequest.get(url).timeout(5000).execute()) {
            JSONObject json = JSONUtil.parseObj(response.body());
            Integer errCode = json.getInt("errcode", 0);
            if (errCode != null && errCode != 0) {
                log.warn("微信 code2Session 失败：{} {}", errCode, json.getStr("errmsg"));
                return null;
            }
            return json;
        } catch (Exception e) {
            log.warn("微信 code2Session 异常：{}", e.getMessage());
            return null;
        }
    }
}
