package org.zcdada.shortlink_zc.admin.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserContext;
import org.zcdada.shortlink_zc.admin.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.admin.common.convention.exception.ServiceException;
import org.zcdada.shortlink_zc.admin.dao.entity.GroupDO;
import org.zcdada.shortlink_zc.admin.dao.entity.GroupUniqueDO;
import org.zcdada.shortlink_zc.admin.dao.mapper.GroupMapper;
import org.zcdada.shortlink_zc.admin.dao.mapper.GroupUniqueMapper;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupOrderReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupSaveReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.ShortLinkGroupRespDTO;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkActualRemoteService;
import org.zcdada.shortlink_zc.admin.remote.ShortLinkRemoteService;
import org.zcdada.shortlink_zc.admin.service.GroupService;
import org.zcdada.shortlink_zc.admin.toolkit.RandomGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zcdada.shortlink_zc.admin.common.constant.RedisCacheConstant.LOCK_GROUP_CREATE_KEY;

/**
 * @Author: zcdada
 * @Description: 短链接分组实现层
 * @DateTime: 2025/11/26 10:25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper,GroupDO> implements GroupService {


    private final RBloomFilter<String> gidRegisterCachePenetrationBloomFilter;
    private final GroupUniqueMapper groupUniqueMapper;
    private final RedissonClient redissonClient;

    @Value("${short-link.group.max-num}")
    private Integer groupMaxNum;

    @Value("${short-link.group.max-retry-num}")
    private Integer groupMaxRetryNum;

    private final ShortLinkActualRemoteService shortLinkRemoteService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGroup(ShortLinkGroupSaveReqDTO requestParam) {
        saveGroup(UserContext.getUsername(),requestParam);
    }

    public void saveGroup(String username,ShortLinkGroupSaveReqDTO requestParam) {

        RLock lock = redissonClient.getLock(String.format(LOCK_GROUP_CREATE_KEY, username));
        lock.lock();
        try {
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(groupDOList) && groupDOList.size() == groupMaxNum) {
                throw new ClientException(String.format("已超出最大分组数：%d", groupMaxNum));
            }
            Integer count = 0;
            String gid = null;
            //查找可用gid
            while (count<groupMaxRetryNum){
                gid = saveGroupUniqueReturnGid();
                if (!gidRegisterCachePenetrationBloomFilter.contains(gid)){
                    break;
                }
                count++;
            }
            if (StrUtil.isEmpty(gid)) {
                throw new ServiceException("生成分组标识频繁");
            }

            GroupDO groupDO = GroupDO.builder()
                .gid(gid)
                .name(requestParam.getName())
                .username(username)
                .sortOrder(0)
                .build();
            baseMapper.insert(groupDO);
            gidRegisterCachePenetrationBloomFilter.add(gid);
        }finally {
            lock.unlock();
        }
    }

    private String saveGroupUniqueReturnGid() {
        String gid = RandomGenerator.generateRandomString();
        if (!gidRegisterCachePenetrationBloomFilter.contains(gid)) {
            GroupUniqueDO groupUniqueDO = GroupUniqueDO.builder()
                    .gid(gid)
                    .build();
            try {
                groupUniqueMapper.insert(groupUniqueDO);
            } catch (DuplicateKeyException e) {
                return null;
            }
        }
        return gid;
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

        LambdaUpdateWrapper<GroupDO> updateWrapper = Wrappers.lambdaUpdate(GroupDO.class)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getDelFlag, 0);
        GroupDO groupDO = new GroupDO();
        groupDO.setDelFlag(1);
        baseMapper.update(groupDO, updateWrapper);
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
