package org.zcdada.shortlink_zc.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zcdada.shortlink_zc.admin.dao.entity.UserDO;
import org.zcdada.shortlink_zc.admin.dto.req.UserLoginReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.UserRegisterReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.UserUpdateDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.UserLoginRespDTO;
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

 
    
    /**
     * @Author: zcdada
     * @Description: 根据用户名修改用户
     * @DateTime: 2025/11/25 16:13
     */
    void update(UserUpdateDTO requestParam);


/**
 * @Author: zcdada
 * @Description: 获取密码 测试用 看md5加密
 * @DateTime: 2025/11/25 16:18
 */
    String getPassword(String username);

    /**
     * @Author: zcdada
     * @Description: 用户登录 返回token
     * @DateTime: 2025/11/25 16:29
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    /**
     * @Author: zcdada
     * @Description: todo 检查用户是否登录 后期用jwt修改
     * @DateTime: 2025/11/25 17:05
     */
    Boolean checkLogin(String username, String token);

    /**
     * @Author: zcdada
     * @Description: 退出登录,删除redis中存的token
     * @DateTime: 2025/11/25 17:12
     */
    void logout(String username, String token);
}
