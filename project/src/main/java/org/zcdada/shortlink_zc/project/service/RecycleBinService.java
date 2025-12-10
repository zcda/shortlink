package org.zcdada.shortlink_zc.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDO;
import org.zcdada.shortlink_zc.project.dto.req.RecycleBinPageReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.RecycleBinSaveReqDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkPageRespDTO;

public interface RecycleBinService  extends IService<ShortLinkDO> {
    /**
     * @Author: zcdada
     * @Description: 将短链接移动到回收站
     * @DateTime: 2025/12/10 11:08
     */
    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);

    IPage<ShortLinkPageRespDTO> pageShortLink(RecycleBinPageReqDTO requestParam);
}
