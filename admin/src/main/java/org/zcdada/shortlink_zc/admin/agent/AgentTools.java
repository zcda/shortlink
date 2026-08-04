package org.zcdada.shortlink_zc.admin.agent;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserContext;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserInfoDTO;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.dto.resp.ShortLinkGroupRespDTO;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkActualRemoteService;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkStatsRespDTO;
import org.zcdada.shortlink_zc.admin.service.GroupService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AgentTools {

    private final ShortLinkActualRemoteService remoteService;

    private final GroupService groupService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Tool(description = """
        创建短链接。
        参数:
        - originUrl: 原始长链接（必填，必须是完整URL如 https://example.com/page）
        - gid: 分组标识（必填，需先通过 getGroupList 获取可用分组的 gid）
        - describe: 短链接描述（可选，不填则自动获取网页标题）
        返回值: 创建成功后的短链接 URL
        """)
    public String createShortLink(String originUrl, String gid, String describe, ToolContext toolContext) {
        if (!setupUserContext(toolContext)) {
            return "无法获取用户信息，请先登录后再操作。";
        }
        try {
            ShortLinkCreateReqDTO req = ShortLinkCreateReqDTO.builder()
                    .originUrl(originUrl)
                    .gid(gid)
                    .describe(describe)
                    .createdType(1)
                    .validDateType(0)
                    .build();
            Result<ShortLinkCreateRespDTO> result = remoteService.createShortLink(req);
            if (result.isSuccess() && result.getData() != null) {
                return "创建成功！短链接: " + result.getData().getFullShortUrl();
            }
            return "创建失败: " + result.getMessage();
        } catch (Exception e) {
            log.error("Agent tool createShortLink error: originUrl={}, gid={}", originUrl, gid, e);
            return "创建短链接时发生错误，请稍后重试。";
        } finally {
            UserContext.removeUser();
        }
    }

    @Tool(description = """
        分页查询指定分组下的短链接列表。
        参数:
        - gid: 分组标识（必填，通过 getGroupList 获取）
        - page: 页码（默认 1）
        - size: 每页条数（默认 10，最大 50）
        返回值: 短链接列表 JSON，包含 url、描述、创建时间、点击量
        """)
    public String listShortLinks(String gid, int page, int size, ToolContext toolContext) {
        if (!setupUserContext(toolContext)) {
            return "无法获取用户信息，请先登录后再操作。";
        }
        try {
            long current = Math.max(1, page);
            long pageSize = Math.min(Math.max(1, size), 50);
            Result<Page<ShortLinkPageRespDTO>> result =
                    remoteService.pageShortLink(gid, "desc", current, pageSize);
            if (result.isSuccess() && result.getData() != null) {
                return JSON.toJSONString(result.getData().getRecords());
            }
            return "查询失败: " + result.getMessage();
        } catch (Exception e) {
            log.error("Agent tool listShortLinks error: gid={}", gid, e);
            return "查询短链接时发生错误，请稍后重试。";
        } finally {
            UserContext.removeUser();
        }
    }

    @Tool(description = """
        查询单个短链接的详细访问统计数据（PV/UV/UIP、浏览器、设备、地区等）。
        参数:
        - fullShortUrl: 完整短链接地址（必填）
        - gid: 所属分组标识（必填）
        返回值: 统计数据摘要
        """)
    public String getShortLinkStats(String fullShortUrl, String gid, ToolContext toolContext) {
        if (!setupUserContext(toolContext)) {
            return "无法获取用户信息，请先登录后再操作。";
        }
        try {
            String today = LocalDate.now().format(DATE_FMT);
            Result<ShortLinkStatsRespDTO> result =
                    remoteService.oneShortLinkStats(fullShortUrl, gid, today, today);
            if (result.isSuccess() && result.getData() != null) {
                return JSON.toJSONString(result.getData());
            }
            return "查询失败: " + result.getMessage();
        } catch (Exception e) {
            log.error("Agent tool getShortLinkStats error: fullShortUrl={}, gid={}", fullShortUrl, gid, e);
            return "查询统计数据时发生错误，请稍后重试。";
        } finally {
            UserContext.removeUser();
        }
    }

    @Tool(description = """
        获取今日所有短链接的访问概览，包含总 PV、UV、UIP。
        无参数。
        返回值: 今日访问统计数据摘要
        """)
    public String getStatsSummary(ToolContext toolContext) {
        if (!setupUserContext(toolContext)) {
            return "无法获取用户信息，请先登录后再操作。";
        }
        try {
            List<ShortLinkGroupRespDTO> groups = groupService.groupList();
            if (groups == null || groups.isEmpty()) {
                return "暂无分组数据。";
            }
            String today = LocalDate.now().format(DATE_FMT);
            int totalPv = 0;
            int totalUv = 0;
            int totalUip = 0;
            for (ShortLinkGroupRespDTO group : groups) {
                try {
                    Result<ShortLinkStatsRespDTO> result =
                            remoteService.groupShortLinkStats(group.getGid(), today, today);
                    if (result.isSuccess() && result.getData() != null) {
                        ShortLinkStatsRespDTO stats = result.getData();
                        totalPv += stats.getPv() != null ? stats.getPv() : 0;
                        totalUv += stats.getUv() != null ? stats.getUv() : 0;
                        totalUip += stats.getUip() != null ? stats.getUip() : 0;
                    }
                } catch (Exception ignored) {
                }
            }
            return String.format("今日访问概览：总 PV=%d, UV=%d, UIP=%d（共 %d 个分组）",
                    totalPv, totalUv, totalUip, groups.size());
        } catch (Exception e) {
            log.error("Agent tool getStatsSummary error", e);
            return "获取今日访问概览时发生错误，请稍后重试。";
        } finally {
            UserContext.removeUser();
        }
    }

    @Tool(description = """
        获取当前登录用户的所有短链接分组列表。
        无参数。
        返回值: 分组列表 JSON，每个分组包含 gid（分组标识）和 name（分组名称）
        """)
    public String getGroupList(ToolContext toolContext) {
        if (!setupUserContext(toolContext)) {
            return "无法获取用户信息，请先登录后再操作。";
        }
        try {
            List<ShortLinkGroupRespDTO> groups = groupService.groupList();
            if (groups == null || groups.isEmpty()) {
                return "暂无分组，请先在控制台创建分组。";
            }
            return JSON.toJSONString(groups);
        } catch (Exception e) {
            log.error("Agent tool getGroupList error", e);
            return "获取分组列表时发生错误，请稍后重试。";
        } finally {
            UserContext.removeUser();
        }
    }

    @Tool(description = """
        分页查询回收站中的短链接。
        参数:
        - page: 页码（默认 1）
        - size: 每页条数（默认 10，最大 50）
        返回值: 回收站列表 JSON
        """)
    public String recycleBinPage(int page, int size, ToolContext toolContext) {
        if (!setupUserContext(toolContext)) {
            return "无法获取用户信息，请先登录后再操作。";
        }
        try {
            List<ShortLinkGroupRespDTO> groups = groupService.groupList();
            List<String> gidList = groups != null
                    ? groups.stream().map(ShortLinkGroupRespDTO::getGid).toList()
                    : Collections.emptyList();
            if (gidList.isEmpty()) {
                return "暂无分组，回收站为空。";
            }
            long current = Math.max(1, page);
            long pageSize = Math.min(Math.max(1, size), 50);
            Result<Page<ShortLinkPageRespDTO>> result =
                    remoteService.pageRecycleBinShortLink(gidList, current, pageSize);
            if (result.isSuccess() && result.getData() != null) {
                return JSON.toJSONString(result.getData().getRecords());
            }
            return "查询失败: " + result.getMessage();
        } catch (Exception e) {
            log.error("Agent tool recycleBinPage error", e);
            return "查询回收站时发生错误，请稍后重试。";
        } finally {
            UserContext.removeUser();
        }
    }

    private boolean setupUserContext(ToolContext toolContext) {
        Object user = toolContext != null ? toolContext.getContext().get("userInfo") : null;
        if (!(user instanceof UserInfoDTO userInfoDTO)) {
            return false;
        }
        UserContext.setUser(userInfoDTO);
        return true;
    }
}
