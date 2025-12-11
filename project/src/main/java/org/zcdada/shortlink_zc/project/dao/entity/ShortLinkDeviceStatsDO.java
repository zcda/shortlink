package org.zcdada.shortlink_zc.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zcdada.shortlink_zc.project.common.database.BaseDO;

import java.util.Date;

/**
 * (TLinkDeviceStats)表实体类
 *
 * @author makejava
 * @since 2025-12-11 20:17:38
 */
@SuppressWarnings("serial")
@TableName("t_link_device_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkDeviceStatsDO extends BaseDO {
//ID
    private Long id;
//完整短链接
    private String fullShortUrl;
//日期
    private Date date;
//访问量
    private Integer cnt;
//访问设备
    private String device;

}

