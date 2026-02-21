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
     * @Description:  使用的短链接的新老用户 key
     * @DateTime: 2025/12/5 19:29
     */
    public static final String SHORT_LINK_STATS_UV_KEY = "short-link:stats:uv:%s";



    /*
     * @Author: zcdada
     * @Description:  使用的短链接的ip key
     * @DateTime: 2025/12/5 19:29
     */
    public static final String SHORT_LINK_STATS_UIP_KEY = "short-link:stats:uip:%s";


    /*
     * @Author: zcdada
     * @Description:  短链接跳转锁 key
     * @DateTime: 2025/12/5 19:29
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "lock_short_link_goto_%s";


    /*
     * @Author: zcdada
     * @Description:  短链接跳转锁 key
     * @DateTime: 2025/12/5 19:29
     */
    public static final String GOTO_NULL_LINK_KEY = "short-link_is-null_goto_%s";

    /**
     * 短链接修改分组 ID 锁前缀 Key
     */
    public static final String LOCK_GID_UPDATE_KEY = "short-link_lock_update-gid_%s";

    /**
     * 短链接延迟队列消费统计 Key
     */
    public static final String DELAY_QUEUE_STATS_KEY = "short-link_delay-queue:stats";

}
