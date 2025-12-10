package org.zcdada.shortlink_zc.project.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.project.common.convention.result.Result;
import org.zcdada.shortlink_zc.project.common.convention.result.Results;
import org.zcdada.shortlink_zc.project.dto.req.RecycleBinSaveReqDTO;
import org.zcdada.shortlink_zc.project.service.RecycleBinService;

/**
 * @Author: zcdada
 * @Description: 回收站的controller
 * @DateTime: 2025/11/27 16:52
 */
@RestController
@RequiredArgsConstructor
public class RecycleBinController {

    private final RecycleBinService recycleBinService;

    /**
     * @Author: zcdada
     * @Description: 将短链接移到回收站
     * @DateTime: 2025/11/26 16:15
     */
    @PostMapping("/api/short-link/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParam){
        recycleBinService.saveRecycleBin(requestParam);
        return Results.success();
    }

}
