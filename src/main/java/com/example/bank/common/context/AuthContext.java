package com.example.bank.common.context;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthContext {
    private final String ip;
    private final String userAgent;


    public static AuthContext from(HttpServletRequest req) {
        String ip        = resolveClientIp(req);
        String userAgent = normalizeUserAgent(req.getHeader("User-Agent"));

        return new AuthContext(ip,userAgent);
    }

    /**
     * Rút gọn User-Agent để tránh log quá dài
     */
    private static String normalizeUserAgent(String ua) {
        if (ua == null) return "";
        return ua.length() > 80 ? ua.substring(0, 80) + "..." : ua;
    }
    /**
     * Lấy IP thật của client, ưu tiên các header từ proxy/CDN trước
     */
    public static String resolveClientIp(HttpServletRequest req) {
        String[] headers = {
                "CF-Connecting-IP",
                "X-Forwarded-For",
                "X-Real-IP",
                "Forwarded",
                "X-Cluster-Client-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };
        // Kiểm tra lần lượt các header để lấy IP client
        for (String h : headers) {
            String v = req.getHeader(h);
            if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v)) {
                return normalizeIp(v.split(",")[0].trim());
            }
        }

        return normalizeIp(req.getRemoteAddr());
    }

    public static String nvl(String s) {
        return s == null ? "" : s;
    }

    // Chuyển IPv6 localhost -> IPv4 cho dễ đọc
    public static String normalizeIp(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip)) return "127.0.0.1";
        return ip;
    }
}
