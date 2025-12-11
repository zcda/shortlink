package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkOsStatsDO;

/**
 * @Author: zcdada
 * @Description: 地区访问持久层
 * @DateTime: 2025/12/11 16:59
 */
public interface ShortLinkOsStatsMapper extends BaseMapper<ShortLinkOsStatsDO> {

    @Insert("""
    INSERT INTO
      t_link_os_stats (
        full_short_url,
        date,
        cnt,
        os,
        create_time,
        update_time,
        del_flag
      )
    VALUES(
        #{LinkOsStats.fullShortUrl},
        #{LinkOsStats.date},
        #{LinkOsStats.cnt},
        #{LinkOsStats.os},
        NOW(),
        NOW(),
        0
      ) ON DUPLICATE KEY
    UPDATE
      cnt = cnt + #{LinkOsStats.cnt};
""")
    void shortLinkOsStats(@Param("LinkOsStats")ShortLinkOsStatsDO shortLinkOsStatsDO);
}
