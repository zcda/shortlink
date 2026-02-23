package org.zcdada.shortlink_zc.admin.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkActualRemoteService;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkRemoteService;


@RestController
@RequiredArgsConstructor
public class ShortLinkTitleController {

    private final ShortLinkActualRemoteService shortLinkRemoteService;

    /**
     * @Author: zcdada
     * @Description: 获取原始链接下的 网站标题
     * @DateTime: 2025/11/26 16:15
     */
    @GetMapping("/api/short-link/admin/v1/title")
    public Result<String> getTitle(@RequestParam("url") String url){
        return shortLinkRemoteService.getTitle(url);
    }



}
