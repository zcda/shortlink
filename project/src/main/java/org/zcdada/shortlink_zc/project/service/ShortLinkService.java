package org.zcdada.shortlink_zc.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkBatchCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkPageReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkUpdateReqDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkBatchCreateRespDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkCreateRespDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkGroupRespDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkPageRespDTO;

import java.util.List;


public interface ShortLinkService extends IService<ShortLinkDO> {
    /**
     * @Author: zcdada
     * @Description: 创建短链接
     * @DateTime: 2025/11/27 17:02
     */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

/**
 * @Author: zcdada
 * @Description: 修改短链接
 * @DateTime: 2025/12/5 10:57
 */
    void updateShortLink(ShortLinkUpdateReqDTO requestParam);

    /**
     * @Author: zcdada
     * @Description: 分页查询短链接
     * @DateTime: 2025/11/28 15:50
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);

    /**
     * @Author: zcdada
     * @Description: 查询对应gid下的count
     * @DateTime: 2025/12/4 12:55
     */
    ShortLinkGroupRespDTO shortLinkCount(List<String> requestParam);


    /**
     * @Author: zcdada
     * @Description: 短链接跳转
     * @DateTime: 2025/12/5 19:40
     */
    void restoreUrl(String shortUri, ServletRequest request, ServletResponse response);

    ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam);
}
