package org.zcdada.shortlink_zc.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.admin.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.admin.dao.entity.UserDO;
import org.zcdada.shortlink_zc.admin.dao.mapper.UserMapper;
import org.zcdada.shortlink_zc.admin.dto.req.UserRegisterReqDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.UserRespDTO;
import org.zcdada.shortlink_zc.admin.service.UserService;

import static org.zcdada.shortlink_zc.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static org.zcdada.shortlink_zc.admin.common.enums.UserErrorCodeEnum.USERNAME_EXIT;
import static org.zcdada.shortlink_zc.admin.common.enums.UserErrorCodeEnum.USE_SAVE_ERROR;

/**
* @Author: zcdada
* @Description: 用户接口实现
* @DateTime: 2025/11/21 18:39
*/
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;

    private final RedissonClient redissonClient;


    @Override
    public UserRespDTO getUserByUserName(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        UserRespDTO Result = new UserRespDTO();
        if (userDO == null) {
            return null;
        }
        BeanUtils.copyProperties(userDO, Result);

        return Result;
    }

    @Override
    public Boolean hasUsername(String username) {
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if(hasUsername(requestParam.getUsername())) {
            throw new ClientException(USERNAME_EXIT);
        }
        //添加分布式锁，防止用户 恶意请求短时间内使用同一用户名大量注册
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        try{
            if (lock.tryLock()) {
                int insert = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));

                if(insert < 1) {
                    throw new ClientException(USE_SAVE_ERROR);
                }
                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
                return;
            }
            throw new ClientException(USERNAME_EXIT);
        }finally {
            lock.unlock();
        }



    }
}
