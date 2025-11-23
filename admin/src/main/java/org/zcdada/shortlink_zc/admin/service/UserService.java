package org.zcdada.shortlink_zc.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zcdada.shortlink_zc.admin.dao.entity.UserDO;
import org.zcdada.shortlink_zc.admin.dto.req.UserRegisterReqDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.UserRespDTO;

/**
* @Author: zcdada
* @Description:用户接口曾
* @DateTime: 2025/11/21 18:38
*/
public interface UserService extends IService<UserDO> {

    /**
    * @Author: zcdada
    * @Description: 根据用户名查询用户信息
    * @DateTime: 2025/11/21 18:44
    */
    UserRespDTO getUserByUserName(String username);

    /**
     * @Author: zcdada
     * @Description: 查看用户名是否可用 存在(不可用)返回true
     * @DateTime: 2025/11/23 15:44
     */
    Boolean hasUsername(String username);

    /**
     * @Author: zcdada
     * @Description: 用户注册
     * @DateTime: 2025/11/23 16:29
     */
    void register(UserRegisterReqDTO requestParam);
}
