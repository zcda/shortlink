package org.zcdada.shortlink_zc.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.project.common.constant.RedisKeyConstant;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDO;
import org.zcdada.shortlink_zc.project.dao.mapper.ShortLinkMapper;
import org.zcdada.shortlink_zc.project.dto.req.RecycleBinPageReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.RecycleBinRecoverReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.RecycleBinSaveReqDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkPageRespDTO;
import org.zcdada.shortlink_zc.project.service.RecycleBinService;

@Slf4j
@RequiredArgsConstructor
@Service
public class RecycleBinServiceImpl  extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements RecycleBinService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParam) {
        //移动到回收站
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl());
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(1)
                .build();
        baseMapper.update(shortLinkDO, updateWrapper);

        //清理缓存
        stringRedisTemplate.delete(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,requestParam.getFullShortUrl()));
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(RecycleBinPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .in(ShortLinkDO::getGid,requestParam.getGidList())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getUpdateTime);

        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);

        return resultPage.convert(each -> {
            each.setDomain("http://" + each.getDomain());
            return BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
        });
    }

    @Override
    public void recoverRecycleBin(RecycleBinRecoverReqDTO requestParam) {

        //移动到回收站
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl());
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(0)
                .build();
        baseMapper.update(shortLinkDO, updateWrapper);


        //删除空白跳转的内存
        stringRedisTemplate.delete(String.format(RedisKeyConstant.GOTO_NULL_LINK_KEY,requestParam.getFullShortUrl()));


        // 看情况需不需要把从回收站恢复的短链接预热
    }
}
