package org.zcdada.shortlink_zc.admin.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: zcdada
 * @Description: 用户登录接口
 * @DateTime: 2025/11/25 16:20
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRespDTO {
    /**
     * @Author: zcdada
     * @Description: 用户token
     * @DateTime: 2025/11/25 16:19
     */
    private String token;
}
