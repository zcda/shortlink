package org.zcdada.shortlink_zc.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zcdada.shortlink_zc.project.common.database.BaseDO;

/**
 * (TLinkAccessLogs)表实体类
 *
 * @author makejava
 * @since 2025-12-11 20:37:25
 */
@SuppressWarnings("serial")
@TableName("t_link_access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkAccessLogsDO extends BaseDO {

//ID
    private Long id;
//完整短链接
    private String fullShortUrl;
//用户信息
    private String user;
//IP
    private String ip;
//浏览器
    private String browser;
//操作系统
    private String os;
//访问网络
    private String network;
//访问设备
    private String device;
//地区
    private String locale;

}

