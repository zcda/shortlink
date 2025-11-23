package org.zcdada.shortlink_zc.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * (t_user)表实体类
 *  用户表
 * @author makejava
 * @since 2025-11-21 17:30:08
 */
@Data
@TableName("t_user")
public class UserDO implements Serializable {
//ID
    private Long id;
//用户名
    private String username;
//密码
    private String password;

    private String realName;
//手机号
    private String phone;

    private String mail;
//注销时间戳
    private Long deletionTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


//删除表示 0:未删除,1:已经删除
@TableField(fill = FieldFill.INSERT)
    private Integer delFlag;


}

