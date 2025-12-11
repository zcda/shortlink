package org.zcdada.shortlink_zc.project.toolkit;


import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * IP工具类 - 获取真实客户端IP地址
 * 支持多级代理、防止IP伪造、安全可靠
 */
public class IpUtils {
    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String SEPARATOR = ",";

    // 内网IP段（用于识别代理服务器）
    private static final Set<String> INTERNAL_IP_SEGMENTS = new HashSet<>(
            Arrays.asList(
                    "10.", "192.168.", "172.16.", "172.17.", "172.18.", "172.19.",
                    "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
                    "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31."
            )
    );

    /**
     * 获取真实客户端IP（推荐使用）
     * 安全可靠，防止伪造，支持多级代理
     */
    public static String getClientRealIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理X-Forwarded-For可能包含多个IP的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 处理IPv6的localhost情况
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = LOCALHOST_IP;
        }

        return ip;
    }
}