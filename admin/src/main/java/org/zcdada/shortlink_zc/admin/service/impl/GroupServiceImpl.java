package org.zcdada.shortlink_zc.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.admin.dao.entity.GroupDO;
import org.zcdada.shortlink_zc.admin.dao.mapper.GroupMapper;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupSaveReqDTO;
import org.zcdada.shortlink_zc.admin.service.GroupService;
import org.zcdada.shortlink_zc.admin.toolkit.RandomGenerator;

/**
 * @Author: zcdada
 * @Description: 短链接分组实现层
 * @DateTime: 2025/11/26 10:25
 */
@Service
@Slf4j
public class GroupServiceImpl extends ServiceImpl<GroupMapper,GroupDO> implements GroupService {
    @Override
    public void saveGroup(ShortLinkGroupSaveReqDTO requestParam) {
        String gid;
        //判断gid是否可用
        while (true) {
            gid =  RandomGenerator.generateRandomString();
            LambdaQueryWrapper<GroupDO> wrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getGid, gid)
                    //todo 查询username
                    .eq(GroupDO::getUsername, null);
            if (baseMapper.selectOne(wrapper) == null) {
                break;
            }
        }
        GroupDO groupDO = GroupDO.builder()
                .gid(gid)
                .name(requestParam.getName())
                .build();
        baseMapper.insert(groupDO);
    }
}
