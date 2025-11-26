package org.zcdada.shortlink_zc.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.zcdada.shortlink_zc.admin.common.database.BaseDO;

import java.io.Serializable;

/**
 * (t_user)表实体类
 *  用户表
 * @author makejava
 * @since 2025-11-21 17:30:08
 */
@Data
@TableName("t_user")
public class UserDO  extends BaseDO implements Serializable{
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

}

