package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkBrowserStatsDO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkGroupStatsReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkStatsReqDTO;

import java.util.HashMap;
import java.util.List;

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


    /**
     * 根据短链接获取指定日期内浏览器监控数据
     */
    @Select("SELECT " +
            "    browser, " +
            "    SUM(cnt) AS count " +
            "FROM " +
            "    t_link_browser_stats " +
            " WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, date, browser;")
    List<HashMap<String, Object>> listBrowserStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


    /**
     * 根据分组获取指定日期内浏览器监控数据
     */
    @Select("SELECT " +
            "    browser, " +
            "    SUM(cnt) AS count " +
            "FROM " +
            "    t_link_browser_stats s left join t_link t on t.full_short_url = s.full_short_url" +
            " WHERE " +
            "    t.gid = #{param.gid} " +
            "    AND s.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    gid, date, browser;")
    List<HashMap<String, Object>> listBrowserStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);

}
