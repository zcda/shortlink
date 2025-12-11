package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkBrowserStatsDO;

/**
 * @Author: zcdada
 * @Description: 地区访问持久层
 * @DateTime: 2025/12/11 16:59
 */
public interface ShortLinkBrowserStatsMapper extends BaseMapper<ShortLinkBrowserStatsDO> {

    @Insert("""
    INSERT INTO
      t_link_browser_stats (
        full_short_url,
        date,
        cnt,
        browser,
        create_time,
        update_time,
        del_flag
      )
    VALUES(
        #{LinkBrowserStats.fullShortUrl},
        #{LinkBrowserStats.date},
        #{LinkBrowserStats.cnt},
        #{LinkBrowserStats.browser},
        NOW(),
        NOW(),
        0
      ) ON DUPLICATE KEY
    UPDATE
      cnt = cnt + #{LinkBrowserStats.cnt};
""")
    void shortLinkBrowserStats(@Param("LinkBrowserStats")ShortLinkBrowserStatsDO shortLinkBrowserStatsDO);
}
