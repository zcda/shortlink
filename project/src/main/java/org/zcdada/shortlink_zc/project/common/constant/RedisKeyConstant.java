package org.zcdada.shortlink_zc.project.common.constant;

public class RedisKeyConstant {
    /*
    * @Author: zcdada
    * @Description:  短链接跳转前缀 key
    * @DateTime: 2025/12/5 19:29
    */
    public static final String GOTO_SHORT_LINK_KEY = "short_link_goto_%s";


    /*
     * @Author: zcdada
     * @Description:  短链接跳转锁 key
     * @DateTime: 2025/12/5 19:29
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "lock_short_link_goto_%s";
}
