package org.zcdada.shortlink_zc.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.common.convention.result.Results;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkRemoteService;
import org.zcdada.shortlink_zc.admin.remote.dto.req.RecycleBinRecoverReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.req.RecycleBinSaveReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.zcdada.shortlink_zc.admin.service.RecycleBinService;

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

    private final RecycleBinService recycleBinService;

    /**
     * @Author: zcdada
     * @Description: 将短链接移到回收站
     * @DateTime: 2025/11/26 16:15
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam){
        shortLinkRemoteService.saveRecycleBin(requestParam);
        return Results.success();
    }


    /**
     * @Author: zcdada
     * @Description: 恢复短链接
     * @DateTime: 2025/12/10 17:17
     */
    @PostMapping("/api/short-link/admin/v1/recycle-bin/recover")
    public Result<Void> recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO requestParam){
        shortLinkRemoteService.recoverRecycleBin(requestParam);
        return Results.success();
    }



    /**
     * @Author: zcdada
     * @Description: 查询用户回收站下的短链接（要先查用户分组）
     * @DateTime: 2025/12/4 12:04
     */
    @GetMapping("/api/short-link/admin/v1/recycle-bin/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(){
        return recycleBinService.pageShortLink();
    }
}
