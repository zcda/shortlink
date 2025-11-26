package org.zcdada.shortlink_zc.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zcdada.shortlink_zc.admin.dao.entity.GroupDO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupOrderReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupSaveReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.ShortLinkGroupRespDTO;

import java.util.List;

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

    /**
     * @Author: zcdada
     * @Description: 查询当前登录用户的所有短链接分组
     * @DateTime: 2025/11/26 11:16
     */
    List<ShortLinkGroupRespDTO> groupList();

    /**
     * @Author: zcdada
     * @Description: 更新组名
     * @DateTime: 2025/11/26 16:20
     */
    void updateGroup(ShortLinkGroupUpdateReqDTO requestParam);

    /**
     * @Author: zcdada
     * @Description: 软删除短链接 分组
     * @DateTime: 2025/11/26 16:40
     */
    void deleteGroup(String gid);

    /**
     * @Author: zcdada
     * @Description: 更新分组顺序
     * @DateTime: 2025/11/26 16:42
     */
    void updateGroupOrder(List<ShortLinkGroupOrderReqDTO> requestParam);
}
