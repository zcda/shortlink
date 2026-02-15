package org.zcdada.shortlink_zc.project.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.zcdada.shortlink_zc.project.common.convention.result.Result;
import org.zcdada.shortlink_zc.project.common.convention.result.Results;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkPageReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkUpdateReqDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkCreateRespDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkGroupRespDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkPageRespDTO;
import org.zcdada.shortlink_zc.project.service.ShortLinkService;

import java.util.List;


/**
 * @Author: zcdada
 * @Description: 短链接的controller
 * @DateTime: 2025/11/27 16:52
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    /**
     * @Author: zcdada
     * @Description: 核心功能 短链接跳转
     * @DateTime: 2025/12/5 17:16
     */
    @GetMapping("/{short-uri}")
    public void restoreUrl(@PathVariable("short-uri") String shortUri, ServletRequest request, ServletResponse response) {
        shortLinkService.restoreUrl(shortUri, request, response);
    }
    /**
     * @Author: zcdada
     * @Description: 新增 短链接
     * @DateTime: 2025/11/26 16:15
     */
    @PostMapping("/api/short-link/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParam){
        return Results.success(shortLinkService.createShortLink(requestParam));
    }

    /**
     * @Author: zcdada
     * @Description: 修改 短链接
     * 只能修改    gid;validDateType;validDate;describe;
     * @DateTime: 2025/11/26 16:15
     */
    @PostMapping("/api/short-link/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam){
        shortLinkService.updateShortLink(requestParam);
        return Results.success();
    }

    /**
     * @Author: zcdada
     * @Description: 接的是 qury类型的参数,也就是直接跟在连接后面的
     * todo  restful风格的 get请求的参数 使用qury也就是拼接在连接后面的参数,不能添加requestBody注解
     * @DateTime: 2025/12/4 12:04
     */
    @GetMapping("/api/short-link/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        return Results.success(shortLinkService.pageShortLink(requestParam));
    }
/**
 * @Author: zcdada
 * @Description: 查询分组id下的短链接数量
 * @DateTime: 2025/12/4 12:53
 */
    @GetMapping("/api/short-link/v1/count")
    public Result<ShortLinkGroupRespDTO> shortLinkCount(@RequestParam("requestParam")List<String> requestParam){
        return Results.success(shortLinkService.shortLinkCount(requestParam));
    }
}
