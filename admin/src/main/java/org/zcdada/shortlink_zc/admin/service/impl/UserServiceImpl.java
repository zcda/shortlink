package org.zcdada.shortlink_zc.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.admin.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.admin.dao.entity.UserDO;
import org.zcdada.shortlink_zc.admin.dao.mapper.UserMapper;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupSaveReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.UserLoginReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.UserRegisterReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.UserUpdateDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.UserLoginRespDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.UserRespDTO;
import org.zcdada.shortlink_zc.admin.service.GroupService;
import org.zcdada.shortlink_zc.admin.service.UserService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.zcdada.shortlink_zc.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static org.zcdada.shortlink_zc.admin.common.enums.UserErrorCodeEnum.*;

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

    private final StringRedisTemplate stringRedisTemplate;

    private final GroupService groupService;


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
                try {
                    //redis 布隆过滤器 被缓存清空的情况
                    int insert=baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
                    if(insert < 1) {
                        throw new ClientException(USE_SAVE_ERROR);
                    }
                }catch (DuplicateKeyException e){
                    throw new ClientException(USER_EXIT);
                }

                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
                groupService.saveGroup(requestParam.getUsername(),new ShortLinkGroupSaveReqDTO("默认分组"));
                return;
            }
            throw new ClientException(USERNAME_EXIT);
        }finally {
            lock.unlock();
        }



    }

    @Override
    public String getPassword(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        return userDO.getPassword();
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        LambdaQueryWrapper<UserDO> eq = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getPassword, requestParam.getPassword())
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(eq);
        if (userDO == null) {
            throw new ClientException(USER_NULL);
        }
        //如果用户已经登录则 返回用户token
        Map<Object ,Object> hasLoginMap = stringRedisTemplate.opsForHash().entries("login_" + requestParam.getUsername());
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登录错误"));
            return new UserLoginRespDTO(token);
        }

        //设置token
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put("login_"+requestParam.getUsername(), uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire("login_"+requestParam.getUsername(),30, TimeUnit.MINUTES);

        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get("login_" + username, token)!=null;
    }

    @Override
    public void logout(String username, String token) {
        if(!checkLogin(username,token)){
            throw new ClientException("用户未登录");
        }
        stringRedisTemplate.opsForHash().delete("login_" + username,token);
    }

    @Override
    public void update(UserUpdateDTO requestParam) {
        //todo 验证当前用户是否为登录用户
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam,UserDO.class),updateWrapper);
    }
}
