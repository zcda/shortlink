package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkLocaleStatsDO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkGroupStatsReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;

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


    /**
     * 根据短链接获取指定日期内省份
     * 删除gid
     */
    @Select("SELECT " +
            "    province, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "    t_link_locale_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, province;")
    List<ShortLinkLocaleStatsDO> listLocaleByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


    /**
     * 根据分组获取指定日期内地区监控数据
     */
    @Select("SELECT " +
            "    province, " +
            "    SUM(cnt) AS cnt " +
            "FROM " +
            "  t_link_locale_stats s left join t_link t on t.full_short_url = s.full_short_url" +
            " WHERE " +
            "    t.gid = #{param.gid} " +
            "    AND s.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    gid, province;")
    List<ShortLinkLocaleStatsDO> listLocaleByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);

}
