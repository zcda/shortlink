package org.zcdada.shortlink_zc.project.dto.req;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDO;

/**
 * @Author: zcdada
 * @Description: 短链接分页请求参数
 * @DateTime: 2025/11/28 15:49
 */
@Data
public class ShortLinkPageReqDTO extends Page<ShortLinkDO> {

    private String gid;

    /**
     * 排序标识
     */
    private String orderTag;
}
