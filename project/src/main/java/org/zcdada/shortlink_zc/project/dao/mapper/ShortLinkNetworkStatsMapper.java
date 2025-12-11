package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkNetworkStatsDO;

/**
 * @Author: zcdada
 * @Description: 地区访问持久层
 * @DateTime: 2025/12/11 16:59
 */
public interface ShortLinkNetworkStatsMapper extends BaseMapper<ShortLinkNetworkStatsDO> {

    @Insert("""
    INSERT INTO
      t_link_network_stats (
        full_short_url,
        date,
        cnt,
        network,
        create_time,
        update_time,
        del_flag
      )
    VALUES(
        #{LinkNetworkStats.fullShortUrl},
        #{LinkNetworkStats.date},
        #{LinkNetworkStats.cnt},
        #{LinkNetworkStats.network},
        NOW(),
        NOW(),
        0
      ) ON DUPLICATE KEY
    UPDATE
      cnt = cnt + #{LinkNetworkStats.cnt};
""")
    void shortLinkNetworkStats(@Param("LinkNetworkStats")ShortLinkNetworkStatsDO shortLinkNetworkStatsDO);
}
