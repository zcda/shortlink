package org.zcdada.shortlink_zc.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.common.convention.result.Results;
import org.zcdada.shortlink_zc.admin.dto.resp.ShortLinkGroupRespDTO;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkActualRemoteService;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkRemoteService;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkBatchCreateReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.*;
import org.zcdada.shortlink_zc.admin.toolkit.EasyExcelWebUtil;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {


    private final ShortLinkActualRemoteService shortLinkRemoteService;
    @GetMapping("/api/short-link/admin/v1/page")
    public Result<Page<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){

        return shortLinkRemoteService.pageShortLink(requestParam.getGid(), requestParam.getOrderTag(), requestParam.getCurrent(), requestParam.getSize());
    }


    /**
     * @Author: zcdada
     * @Description: 新增 短链接
     * @DateTime: 2025/11/26 16:15
     */
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return shortLinkRemoteService.createShortLink(requestParam);
    }

    /**
     * 批量创建短链接
     */
    @SneakyThrows
    @PostMapping("/api/short-link/admin/v1/create/batch")
    public void batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam, HttpServletResponse response) {
        Result<ShortLinkBatchCreateRespDTO> shortLinkBatchCreateRespDTOResult = shortLinkRemoteService.batchCreateShortLink(requestParam);
        if (shortLinkBatchCreateRespDTOResult.isSuccess() && shortLinkBatchCreateRespDTOResult.getData() != null) {
            List<ShortLinkBaseInfoRespDTO> baseLinkInfos = shortLinkBatchCreateRespDTOResult.getData().getBaseLinkInfos();
            EasyExcelWebUtil.write(response, "批量创建短链接-SaaS短链接系统", ShortLinkBaseInfoRespDTO.class, baseLinkInfos == null ? Collections.emptyList() : baseLinkInfos);
        }
    }
    /**
     * @Author: zcdada
     * @Description: 新增 短链接
     * @DateTime: 2025/11/26 16:15
     */
    @PostMapping("/api/short-link/admin/v1/update")
    public Result<Void> createShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam){
        shortLinkRemoteService.updateShortLink(requestParam);
        return Results.success();
    }



    /**
     * @Author: zcdada
     * @Description: 查询分组对应下的 数量
     * @DateTime: 2025/12/4 20:04
     */
    @GetMapping("/api/short-link/admin/v1/count")
    public Result<ShortLinkGroupRemoteRespDTO> shortLinkCount(@RequestParam("requestParam") List<String> requestParam){

        return shortLinkRemoteService.shortLinkCount(requestParam);
    }
}
