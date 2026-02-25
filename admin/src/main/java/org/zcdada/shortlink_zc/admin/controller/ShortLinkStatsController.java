package org.zcdada.shortlink_zc.admin.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkActualRemoteService;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkRemoteService;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkGroupStatsAccessRecordReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkGroupStatsReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkStatsAccessRecordReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkStatsReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkStatsAccessRecordRespDTO;
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
    private final ShortLinkActualRemoteService shortLinkRemoteService;

    /**
     * 访问单个短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats")
    public Result<ShortLinkStatsRespDTO> shortLinkStats(ShortLinkStatsReqDTO requestParam) {
        return shortLinkRemoteService.oneShortLinkStats(requestParam.getFullShortUrl(),requestParam.getGid(),requestParam.getStartDate(),requestParam.getEndDate());
    }

    /**
     * 访问单个短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record")
    public Result<Page<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam) {
        return shortLinkRemoteService.shortLinkStatsAccessRecord(requestParam.getFullShortUrl(),requestParam.getGid(),requestParam.getStartDate(),requestParam.getEndDate(), requestParam.getCurrent(), requestParam.getSize());
    }



    /**
     * 访问分组短链接指定时间内访问记录监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/access-record/group")
    public Result<Page<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        return shortLinkRemoteService.groupShortLinkStatsAccessRecord(requestParam.getGid(),requestParam.getStartDate(),requestParam.getEndDate());
    }

    /**
     * 访问分组短链接指定时间内监控数据
     */
    @GetMapping("/api/short-link/admin/v1/stats/group")
    public Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        return shortLinkRemoteService.groupShortLinkStats(requestParam.getGid(),requestParam.getStartDate(),requestParam.getEndDate());
    }

}
