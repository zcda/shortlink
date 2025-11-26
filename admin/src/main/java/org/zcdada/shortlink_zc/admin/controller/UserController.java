package org.zcdada.shortlink_zc.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.common.convention.result.Results;
import org.zcdada.shortlink_zc.admin.dto.req.UserLoginReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.UserRegisterReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.UserUpdateDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.UserLoginRespDTO;
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
/**
 * @Author: zcdada
 * @Description: 获取用户未脱敏信息
 * @DateTime:  2025/11/25 16:34
 */
    @GetMapping("/api/short-link/v1/actual/user/{username}")
    public Result<UserRespRealDTO> getActualUserByUsername(@PathVariable("username") String username){
        return Results.success(BeanUtil.toBean(userService.getUserByUserName(username), UserRespRealDTO.class));
    }
/**
 * @Author: zcdada
 * @Description: 判断用户是否存在
 * @DateTime: 2025/11/25 16:34
 */
    @GetMapping("/api/short-link/v1/user/has-username/{username}")
    public Result<Boolean> hasUsername(@PathVariable("username") String username){
        return Results.success(userService.hasUsername(username));
    }
/**
 * @Author: zcdada
 * @Description: 注册
 * @DateTime: 2025/11/25 16:34
 */
    @PostMapping("/api/short-link/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

/**
 * @Author: zcdada
 * @Description: 更新用户信息
 * @DateTime: 2025/11/25 16:34
 */
    @PutMapping("/api/short-link/v1/user")
    public Result<Void> update(@RequestBody UserUpdateDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

/**
 * @Author: zcdada
 * @Description: 登录
 * @DateTime: 2025/11/25 16:37
 */
    @PostMapping("/api/short-link/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userService.login(requestParam));
    }

    /**
     * @Author: zcdada
     * @Description: todo 登录后应该就不用传username了,可以在上下文中获取,通过将 token和username放在请求的header里面
     * @DateTime: 2025/11/26 16:19
     */
    @GetMapping("/api/short-link/v1/user/login")
    public Result<Boolean> checkLogin(@RequestParam("token") String token,@RequestParam("username")String username){
        return Results.success(userService.checkLogin(username,token));
    }


    @DeleteMapping("/api/short-link/v1/user")
    public  Result<Void> logout(@RequestParam("token") String token,@RequestParam("username")String username){
        userService.logout(username,token);
        return  Results.success();
    }


    @GetMapping("/api/short-link/v1/password/{username}")
    public Result<String> getPasswordByUsername(@PathVariable("username") String username){
        return Results.success(userService.getPassword(username));

    }



}
