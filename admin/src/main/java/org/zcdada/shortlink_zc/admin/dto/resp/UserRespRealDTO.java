package org.zcdada.shortlink_zc.admin.dto.resp;

import lombok.Data;

/**
* @Author: zcdada
* @Description: 用户返回参数响应
* @DateTime: 2025/11/21 18:42
*/
@Data
public class UserRespRealDTO {


    //ID
    private Long id;
    //用户名
    private String username;

    private String realName;
    /**
     * 手机号
     */
    private String phone;

    private String mail;


}
