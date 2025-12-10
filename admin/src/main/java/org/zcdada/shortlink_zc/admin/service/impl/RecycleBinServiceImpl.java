package org.zcdada.shortlink_zc.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserContext;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.dao.entity.GroupDO;
import org.zcdada.shortlink_zc.admin.dao.mapper.GroupMapper;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkRemoteService;
import org.zcdada.shortlink_zc.admin.remote.dto.req.RecycleBinPageReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.zcdada.shortlink_zc.admin.service.RecycleBinService;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class RecycleBinServiceImpl  extends ServiceImpl<GroupMapper, GroupDO> implements RecycleBinService {
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };


    @Override
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink() {

        LambdaQueryWrapper<GroupDO> wrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag, 0);
        List<GroupDO> groupDOS = baseMapper.selectList(wrapper);
        RecycleBinPageReqDTO requestParam =  new RecycleBinPageReqDTO();
        requestParam.setGidList(groupDOS.stream().map(GroupDO::getGid).toList());
        return shortLinkRemoteService.pageShortLinkRecycleBin(requestParam);
    }
}
