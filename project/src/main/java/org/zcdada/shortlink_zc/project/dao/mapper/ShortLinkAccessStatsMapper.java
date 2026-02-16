package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkAccessStatsDO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkGroupStatsReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkStatsReqDTO;

import java.util.List;
public interface ShortLinkAccessStatsMapper extends BaseMapper<ShortLinkAccessStatsDO> {

    @Insert("INSERT INTO t_link_access_stats (full_short_url,date,pv,uv,uip,hour,weekday,create_time,update_time,del_flag)" +
            "VALUES (#{LinkAccessStats.fullShortUrl},#{LinkAccessStats.date},#{LinkAccessStats.pv},#{LinkAccessStats.uv},#{LinkAccessStats.uip},#{LinkAccessStats.hour},#{LinkAccessStats.weekday},NOW(),NOW(),0)" +
            "ON DUPLICATE KEY " +
            "UPDATE pv=pv+#{LinkAccessStats.pv},uv=uv+#{LinkAccessStats.uv},uip=uip+#{LinkAccessStats.uip}")
    void shortLinkStats(@Param("LinkAccessStats")ShortLinkAccessStatsDO shortLinkAccessStats);


    /**
     * 根据短链接获取指定日期内基础监控数据
     * 后续gid改成全局唯一，删除gid
     */
    @Select("SELECT " +
            "    date, " +
            "    SUM(pv) AS pv, " +
            "    SUM(uv) AS uv, " +
            "    SUM(uip) AS uip " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, date;")
    List<ShortLinkAccessStatsDO> listStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


    /**
     * 根据短链接获取指定日期内小时基础监控数据(某个指定小时)
     */
    @Select("SELECT " +
            "    hour, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats " +
            "WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +

            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, hour;")
    List<ShortLinkAccessStatsDO> listHourStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


    /**
     * 根据短链接获取指定日期内星期基础监控数据（pv）
     */
    @Select("SELECT " +
            "    weekday, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats " +
            " WHERE " +
            "    full_short_url = #{param.fullShortUrl} " +
            "    AND date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    full_short_url, weekday;")
    List<ShortLinkAccessStatsDO> listWeekdayStatsByShortLink(@Param("param") ShortLinkStatsReqDTO requestParam);


    /**
     * 根据分组获取指定日期内基础监控数据
     * todo 访问表中已经移除gid字段,所以需要连表查?
     */
    @Select("SELECT " +
            "    date, " +
            "    SUM(pv) AS pv, " +
            "    SUM(uv) AS uv, " +
            "    SUM(uip) AS uip " +
            "FROM " +
            "    t_link_access_stats s left join t_link t on t.full_short_url = s.full_short_url " +
            " WHERE " +
            "    t.gid = #{param.gid} " +
            "    AND s.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    gid, date;")
    List<ShortLinkAccessStatsDO> listStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);


    /**
     * 根据分组获取指定日期内小时基础监控数据
     */
    @Select("SELECT " +
            "    hour, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats s left join t_link t on t.full_short_url = s.full_short_url" +
            " WHERE " +
            "    t.gid = #{param.gid} " +
            "    AND s.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    gid, hour;")
    List<ShortLinkAccessStatsDO> listHourStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);


    /**
     * 根据分组获取指定日期内小时基础监控数据
     */
    @Select("SELECT " +
            "    weekday, " +
            "    SUM(pv) AS pv " +
            "FROM " +
            "    t_link_access_stats s left join t_link t on t.full_short_url = s.full_short_url" +
            " WHERE " +
            "    t.gid = #{param.gid} " +
            "    AND s.date BETWEEN #{param.startDate} and #{param.endDate} " +
            "GROUP BY " +
            "    gid, weekday;")
    List<ShortLinkAccessStatsDO> listWeekdayStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO requestParam);

}
