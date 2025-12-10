package org.zcdada.shortlink_zc.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.common.convention.result.Results;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkRemoteService;
import org.zcdada.shortlink_zc.admin.remote.dto.req.RecycleBinSaveReqDTO;

/**
 * @Author: zcdada
 * @Description: 回收站 控制层
 * @DateTime: 2025/11/26 10:26
 */
@RestController
@RequiredArgsConstructor
public class RecycleBinController {
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };


    /**
     * @Author: zcdada
     * @Description: 将短链接移到回收站
     * @DateTime: 2025/11/26 16:15
     */
    @PostMapping("/api/short-link/v1/admin/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam){
        shortLinkRemoteService.saveRecycleBin(requestParam);
        return Results.success();
    }
}
