package org.zcdada.shortlink_zc.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.common.convention.result.Results;
import org.zcdada.shortlink_zc.admin.dto.req.UserRegisterReqDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.UserRespDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.UserRespRealDTO;
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

    @GetMapping("/api/short-link/v1/actual/user/{username}")
    public Result<UserRespRealDTO> getActualUserByUsername(@PathVariable("username") String username){
        return Results.success(BeanUtil.toBean(userService.getUserByUserName(username), UserRespRealDTO.class));
    }

    @GetMapping("/api/short-link/v1/user/has-username/{username}")
    public Result<Boolean> hasUsername(@PathVariable("username") String username){
        return Results.success(userService.hasUsername(username));
    }

    @PostMapping("/api/short-link/v1/user/register")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

}
