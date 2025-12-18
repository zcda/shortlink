package org.zcdada.shortlink_zc.admin.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkRemoteService;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkStatsReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkStatsRespDTO;

/**
 * 短链接监控控制层
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkStatsController {

    /**
     * 后续重构为 SpringCloud Feign 调用
     */
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    /**
     * 访问单个短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        return shortLinkRemoteService.oneShortLinkStats(requestParam);
    }
}
