package org.zcdada.shortlink_zc.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zcdada.shortlink_zc.project.common.database.BaseDO;

import java.util.Date;

/**
 * (TLinkAccessStats)表实体类
 *
 * @author makejava
 * @since 2025-12-11 10:29:22
 */
@TableName("t_link_access_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkAccessStatsDO extends BaseDO {
//ID
    private Long id;
//完整短链接
    private String fullShortUrl;
//日期
    private Date date;
//访问量
    private Integer pv;
//独立访客数
    private Integer uv;
//独立IP数
    private Integer uip;
//小时
    private Integer hour;
//星期
    private Integer weekday;


}

