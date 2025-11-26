package org.zcdada.shortlink_zc.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zcdada.shortlink_zc.admin.dao.entity.GroupDO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupSaveReqDTO;

/**
 * @Author: zcdada
 * @Description: 短链接分组 接口层
 * @DateTime: 2025/11/26 10:27
 */
public interface GroupService extends IService<GroupDO> {
    /**
     * @Author: zcdada
     * @Description: 新增短链接分组
     * @DateTime: 2025/11/26 10:28
     */
    void saveGroup(ShortLinkGroupSaveReqDTO requestParam);
}
