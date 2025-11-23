package org.zcdada.shortlink_zc.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.admin.dao.entity.UserDO;
import org.zcdada.shortlink_zc.admin.dao.mapper.UserMapper;
import org.zcdada.shortlink_zc.admin.dto.resp.UserRespDTO;
import org.zcdada.shortlink_zc.admin.service.UserService;

/**
* @Author: zcdada
* @Description: 用户接口实现
* @DateTime: 2025/11/21 18:39
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    @Override
    public UserRespDTO getUserByUserName(String userName) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, userName);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        UserRespDTO Result = new UserRespDTO();
        if (userDO == null) {
            return null;
        }
        BeanUtils.copyProperties(userDO, Result);

        return Result;
    }
}
