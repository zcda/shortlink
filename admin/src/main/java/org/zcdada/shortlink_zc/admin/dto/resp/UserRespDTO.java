package org.zcdada.shortlink_zc.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.zcdada.shortlink_zc.admin.common.serialize.PhoneDesensitizationSerializer;

/**
* @Author: zcdada
* @Description: 用户返回参数响应
* @DateTime: 2025/11/21 18:42
*/
@Data
public class UserRespDTO {


    //ID
    private Long id;
    //用户名
    private String username;

    private String realName;
    /**
     * 手机号
     */
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;

    private String mail;


}
