package org.zcdada.shortlink_zc.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (TLinkGoto0)实体类
 *
 * @author makejava
 * @since 2025-12-05 16:58:36
 */
@TableName("t_link_goto")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkGotoDO {

/**
     * ID
     */
    private Long id;
/**
     * 分组标识
     */
    private String gid;
/**
     * 完整短链接
     */
    private String fullShortUrl;



}

