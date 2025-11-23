package org.zcdada.shortlink_zc.admin.dto.req;

import lombok.Data;

/**
 * @Author: zcdada
 * @Description: 用户注册 入参
 * @DateTime: 2025/11/23 16:27
 */
@Data
public class UserRegisterReqDTO {
    //用户名
    private String username;
    //密码
    private String password;

    private String realName;
    //手机号
    private String phone;

    private String mail;
}
