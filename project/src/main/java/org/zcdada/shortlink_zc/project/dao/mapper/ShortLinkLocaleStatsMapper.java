package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkLocaleStatsDO;
/**
 * @Author: zcdada
 * @Description: 地区访问持久层
 * @DateTime: 2025/12/11 16:59
 */
public interface ShortLinkLocaleStatsMapper extends BaseMapper<ShortLinkLocaleStatsDO> {

    @Insert("""
    INSERT INTO
      t_link_locale_stats (
        full_short_url,
        date,
        cnt,
        province,
        city,
        adcode,
        country,
        create_time,
        update_time,
        del_flag
      )
    VALUES(
        #{LinkLocaleStats.fullShortUrl},
        #{LinkLocaleStats.date},
        #{LinkLocaleStats.cnt},
        #{LinkLocaleStats.province},
        #{LinkLocaleStats.city},
        #{LinkLocaleStats.adcode},
        #{LinkLocaleStats.country},
        NOW(),
        NOW(),
        0
      ) ON DUPLICATE KEY
    UPDATE
      cnt = cnt + #{LinkLocaleStats.cnt};
""")
    void shortLinkLocaleStats(@Param("LinkLocaleStats")ShortLinkLocaleStatsDO shortLinkLocaleStatsDO);
}
