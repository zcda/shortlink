package org.zcdada.shortlink_zc.admin.dto.req;

import lombok.Data;

@Data
public class UserUpdateDTO {

    //用户名
    private String username;
    //密码
    private String password;

    private String realName;
    //手机号
    private String phone;

    private String mail;

}
