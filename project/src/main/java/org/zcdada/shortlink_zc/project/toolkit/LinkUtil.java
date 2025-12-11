package org.zcdada.shortlink_zc.project.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

import static org.zcdada.shortlink_zc.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_TIME;

/**
 * @Author: zcdada
 * @Description: 短链接工具类
 * @DateTime: 2025/12/5 20:35
 */
public class LinkUtil {


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



    public static Long getLinkCacheValidTime(Date date) {
        return Optional.ofNullable(date)
                .map(each->DateUtil.between(new Date(),each, DateUnit.MS))
                .orElse(DEFAULT_CACHE_TIME);
    }



    public static String getOperatingSystem(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String os = "Unknown";

        // 检查操作系统
        if (userAgent.toLowerCase().contains("windows")) {
            os = "Windows";
        } else if (userAgent.toLowerCase().contains("mac")) {
            os = "Mac";
        } else if (userAgent.toLowerCase().contains("linux")) {
            os = "Linux";
        } else if (userAgent.toLowerCase().contains("android")) {
            os = "Android";
        } else if (userAgent.toLowerCase().contains("iphone") || userAgent.toLowerCase().contains("ipad")) {
            os = "iOS";
        } else if (userAgent.toLowerCase().contains("ipod")) {
            os = "iOS";
        } else if (userAgent.toLowerCase().contains("win") ||
                userAgent.toLowerCase().contains("windows nt")) {
            os = "Windows";
        }

        return os;
    }


    public static String getBrowser(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        // 检查浏览器类型
        if (userAgent.contains("Edg")) {
            return "Edge";}
        else if (userAgent.contains("Chrome")) {
            return "Chrome";
        }
        else if (userAgent.contains("Safari")) {
            return "Safari";
        }else if (userAgent.contains("Firefox")) {
            return "Firefox";
        }else if (userAgent.contains("Opera") || userAgent.contains("OPR")) {
            return "Opera";
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            return "Internet Explorer";
        } else if (userAgent.contains("Mobile")) {
            return "Mobile Browser";
        }

        return "Unknown Browser";
    }


}
