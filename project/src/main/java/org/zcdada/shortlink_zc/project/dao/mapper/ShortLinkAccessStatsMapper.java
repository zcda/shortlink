package org.zcdada.shortlink_zc.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkAccessStatsDO;

public interface ShortLinkAccessStatsMapper extends BaseMapper<ShortLinkAccessStatsDO> {

    @Insert("INSERT INTO t_link_access_stats (full_short_url,date,pv,uv,uip,hour,weekday,create_time,update_time,del_flag)" +
            "VALUES (#{LinkAccessStats.fullShortUrl},#{LinkAccessStats.date},#{LinkAccessStats.pv},#{LinkAccessStats.uv},#{LinkAccessStats.uip},#{LinkAccessStats.hour},#{LinkAccessStats.weekday},NOW(),NOW(),0)" +
            "ON DUPLICATE KEY " +
            "UPDATE pv=pv+#{LinkAccessStats.pv},uv=uv+#{LinkAccessStats.uv},uip=uip+#{LinkAccessStats.uip}")
    void shortLinkStats(@Param("LinkAccessStats")ShortLinkAccessStatsDO shortLinkAccessStats);
}
