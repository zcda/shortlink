package org.zcdada.shortlink_zc.admin.common.constant;

/**
 * @Author: zcdada
 * @Description: 短链接后台管理 Redis 缓存常量类
 * @DateTime: 2025/11/23 17:09
 */
public class RedisCacheConstant {

    /**
     * 用户注册分布式锁
     */
    public static final String LOCK_USER_REGISTER_KEY = "short-link:lock_user-register:";

    /**
     * 分组创建分布式锁
     */
    public static final String LOCK_GROUP_CREATE_KEY = "short-link:lock_group-create:%s";


    /**
     * 用户登录缓存标识
     */
    public static final String USER_LOGIN_KEY = "short-link:login:";

}
