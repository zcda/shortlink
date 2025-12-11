package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDeviceStatsDO;

/**
 * @Author: zcdada
 * @Description: 地区访问持久层
 * @DateTime: 2025/12/11 16:59
 */
public interface ShortLinkDeviceStatsMapper extends BaseMapper<ShortLinkDeviceStatsDO> {

    @Insert("""
    INSERT INTO
      t_link_device_stats (
        full_short_url,
        date,
        cnt,
        device,
        create_time,
        update_time,
        del_flag
      )
    VALUES(
        #{LinkDeviceStats.fullShortUrl},
        #{LinkDeviceStats.date},
        #{LinkDeviceStats.cnt},
        #{LinkDeviceStats.device},
        NOW(),
        NOW(),
        0
      ) ON DUPLICATE KEY
    UPDATE
      cnt = cnt + #{LinkDeviceStats.cnt};
""")
    void shortLinkDeviceStats(@Param("LinkDeviceStats")ShortLinkDeviceStatsDO shortLinkDeviceStatsDO);
}
