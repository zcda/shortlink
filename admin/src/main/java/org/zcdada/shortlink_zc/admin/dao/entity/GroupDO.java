package org.zcdada.shortlink_zc.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 短链接分组实体
 *
 * @author makejava
 * @since 2025-11-26 10:21:37
 */
@Data
@TableName("t_group")
public class GroupDO {
//ID
    private Long id;
//分组标识
    private String gid;
//分组名称
    private String name;
//创建分组用户名
    private String username;
//分组排序
    private Integer sortOrder;
//创建时间
    private Date createTime;
//修改时间
    private Date updateTime;
//删除标识 0：未删除 1：已删除
    private Integer delFlag;


}

