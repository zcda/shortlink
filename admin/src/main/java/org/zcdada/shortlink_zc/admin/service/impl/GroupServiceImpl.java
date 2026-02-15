package org.zcdada.shortlink_zc.admin.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserContext;
import org.zcdada.shortlink_zc.admin.dao.entity.GroupDO;
import org.zcdada.shortlink_zc.admin.dao.mapper.GroupMapper;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupOrderReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupSaveReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.ShortLinkGroupRespDTO;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkRemoteService;
import org.zcdada.shortlink_zc.admin.service.GroupService;
import org.zcdada.shortlink_zc.admin.toolkit.RandomGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: zcdada
 * @Description: 短链接分组实现层
 * @DateTime: 2025/11/26 10:25
 */
@Service
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper,GroupDO> implements GroupService {

    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    @Override
    public void saveGroup(ShortLinkGroupSaveReqDTO requestParam) {
        saveGroup(UserContext.getUsername(),requestParam);
    }

    public void saveGroup(String username,ShortLinkGroupSaveReqDTO requestParam) {
        String gid;
        //判断gid是否可用
        while (true) {
            gid =  RandomGenerator.generateRandomString();
            LambdaQueryWrapper<GroupDO> wrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getGid, gid)
                    .eq(GroupDO::getUsername, username);
            if (baseMapper.selectOne(wrapper) == null) {
                break;
            }
        }
        GroupDO groupDO = GroupDO.builder()
                .gid(gid)
                .name(requestParam.getName())
                .username(username)
                .sortOrder(0)
                .build();
        baseMapper.insert(groupDO);
    }
    @Override
    public List<ShortLinkGroupRespDTO> groupList() {

        Wrapper<GroupDO> wrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag,0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .orderByDesc(Arrays.asList(GroupDO::getSortOrder,GroupDO::getUpdateTime));

        List<GroupDO> groupList = baseMapper.selectList(wrapper);
        Map<String, Integer> count = shortLinkRemoteService
                .shortLinkCount(groupList.stream().map(GroupDO::getGid).collect(Collectors.toList())).getData().getShortLinkGroupRespDTOS();

        List<ShortLinkGroupRespDTO> result = BeanUtil.copyToList(groupList, ShortLinkGroupRespDTO.class);
        result.forEach(each -> {
            each.setShortLinkCount(count.get(each.getGid()));
        });

        return result;
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO requestParam) {

        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getGid, requestParam.getGid())
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag, 0);

        GroupDO groupDO = new GroupDO();
        groupDO.setName(requestParam.getName());

        baseMapper.update(groupDO, updateWrapper);

    }

    @Override
    public void deleteGroup(String gid) {

        // 如果启用了逻辑删除，应该这样操作
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag, 0);

        // 使用逻辑删除
        int deleteCount = baseMapper.delete(queryWrapper);
    }

    @Override
    public void updateGroupOrder(List<ShortLinkGroupOrderReqDTO> requestParam) {
        requestParam.forEach(item -> {
            GroupDO groupDO = new GroupDO();
            groupDO.setSortOrder(item.getSortOrder());
            LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                    .eq(GroupDO::getGid, item.getGid())
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getDelFlag, 0);
            baseMapper.update(groupDO, updateWrapper);
        });
    }
}
