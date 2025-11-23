package org.zcdada.shortlink_zc.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zcdada.shortlink_zc.admin.dao.entity.UserDO;
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
    UserRespDTO getUserByUserName(String userName);
}
