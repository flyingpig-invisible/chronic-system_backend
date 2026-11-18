package com.gzu.chronic.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 无依赖的轻量 token：base64url(userId|expireAt) + "." + base64url(HMAC-SHA256)
 * 生产环境建议换成 JWT + 刷新令牌，这里够演示与联调。
 */
public final class TokenUtil {

    private static final String SECRET = "chronic-demo-secret-2026";
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private TokenUtil() {
    }

    public static String create(String userId, long ttlSeconds) {
        long exp = System.currentTimeMillis() / 1000 + ttlSeconds;
        String payload = userId + "|" + exp;
        return ENC.encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + ENC.encodeToString(hmac(payload));
    }

    /** 校验通过返回 userId，否则返回 null */
    public static String verify(String token) {
        try {
            if (token == null) {
                return null;
            }
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return null;
            }
            String payload = new String(DEC.decode(parts[0]), StandardCharsets.UTF_8);
            byte[] sig = DEC.decode(parts[1]);
            if (!Arrays.equals(sig, hmac(payload))) {
                return null;
            }
            String[] kv = payload.split("\\|");
            if (Long.parseLong(kv[1]) < System.currentTimeMillis() / 1000) {
                return null;
            }
            return kv[0];
        } catch (Exception e) {
            return null;
        }
    }

    public static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
