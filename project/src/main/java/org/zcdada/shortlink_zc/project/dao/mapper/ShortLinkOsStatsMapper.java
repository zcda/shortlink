package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkOsStatsDO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkStatsReqDTO;

import java.util.HashMap;
import java.util.List;

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

    /**
     * 根据短链接获取指定日期内操作系统监控数据
     */
    @Select("SELECT " +
            "    os, " +
            "    SUM(cnt) AS count " +
            "FROM " +
            "    t_link_os_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, date, os;")
    List<HashMap<String, Object>> listOsStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


}
