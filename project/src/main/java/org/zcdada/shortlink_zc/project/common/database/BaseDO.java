package org.zcdada.shortlink_zc.project.common.database;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class BaseDO {
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    //删除表示 0:未删除,1:已经删除
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
