package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDeviceStatsDO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkGroupStatsReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;

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

    /**
     * 根据短链接获取指定日期内访问设备监控数据
     */
    @Select("SELECT " +
            "    device, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_device_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, device;")
    List<ShortLinkDeviceStatsDO> listDeviceStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


    /**
     * 根据分组获取指定日期内访问设备监控数据
     */
    @Select("SELECT " +
            "    device, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_device_stats s left join t_link t on t.full_short_url = s.full_short_url" +
            " WHERE " +
            "    t.gid = #{param.gid} " +
            "    AND s.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    gid, device;")
    List<ShortLinkDeviceStatsDO> listDeviceStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);

}
