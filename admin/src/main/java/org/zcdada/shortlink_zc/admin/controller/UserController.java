package org.zcdada.shortlink_zc.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.common.convention.result.Results;
import org.zcdada.shortlink_zc.admin.dto.resp.UserRespDTO;
import org.zcdada.shortlink_zc.admin.service.UserService;

@RestController
@RequiredArgsConstructor
public class UserController {
    public final UserService userService;

    /**
    * @Author: zcdada
    * @Description: 根据用户名查询信息
    * @DateTime: 2025/11/21 16:19
    */
    @GetMapping("/api/short-link/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username){
        return Results.success(userService.getUserByUserName(username));

    }
}
