package org.zcdada.shortlink_zc.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkCreateRespDTO;

@Service
public interface ShortLinkService extends IService<ShortLinkDO> {
    /**
     * @Author: zcdada
     * @Description: 创建短链接
     * @DateTime: 2025/11/27 17:02
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);
}
