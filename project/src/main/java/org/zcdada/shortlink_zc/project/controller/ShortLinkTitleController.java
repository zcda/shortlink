package org.zcdada.shortlink_zc.project.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.project.common.convention.result.Result;
import org.zcdada.shortlink_zc.project.common.convention.result.Results;
import org.zcdada.shortlink_zc.project.service.UrlService;

@RestController
@RequiredArgsConstructor
public class ShortLinkTitleController {

    private final UrlService urlService;

    /**
     * @Author: zcdada
     * @Description: 获取原始链接下的 网站标题
     * @DateTime: 2025/11/26 16:15
     */
    @GetMapping("/api/short-link/v1/title")
    public Result<String> getTitle(@RequestParam("url") String url){
        return Results.success(urlService.getTitle(url));
    }



}
