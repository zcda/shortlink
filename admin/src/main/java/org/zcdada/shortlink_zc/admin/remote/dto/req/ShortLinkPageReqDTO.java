package org.zcdada.shortlink_zc.admin.remote.dto.req;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * @Author: zcdada
 * @Description: 短链接分页请求参数
 * @DateTime: 2025/11/28 15:49
 */
@Data
public class ShortLinkPageReqDTO extends Page {

    private String gid;

    /**
     * 排序标识
     */
    private String orderTag;
}
