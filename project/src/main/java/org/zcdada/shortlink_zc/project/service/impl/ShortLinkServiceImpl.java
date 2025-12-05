package org.zcdada.shortlink_zc.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.project.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.project.common.convention.exception.ServiceException;
import org.zcdada.shortlink_zc.project.common.enums.VailDateTypeEnum;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDO;
import org.zcdada.shortlink_zc.project.dao.mapper.ShortLinkMapper;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkPageReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkUpdateReqDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkCreateRespDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkGroupRespDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkPageRespDTO;
import org.zcdada.shortlink_zc.project.service.ShortLinkService;
import org.zcdada.shortlink_zc.project.toolkit.RandomGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class ShortLinkServiceImpl  extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortLinkCachePenetrationBloomFilter;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        // todo 检查是否为当前用户的gid且存在
        String shortLinkUri = getShortLink(requestParam);

        ShortLinkDO shortLinkDO = new ShortLinkDO().builder()
                .gid(requestParam.getGid())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .createdType(requestParam.getCreatedType())
                .domain(requestParam.getDomain())
                .enableStatus(0)
                .originUrl(requestParam.getOriginUrl())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .shortUri(shortLinkUri)
                .fullShortUrl("http://"+requestParam.getDomain() + "/" + shortLinkUri)
                .build();


        try{
            baseMapper.insert(shortLinkDO);

        }catch (Exception e){
            log.warn("重复短链接");
            System.out.println(e.getMessage());
            throw new ServiceException("重复短链接,请稍后再试试");
        }

        return BeanUtil.toBean(shortLinkDO, ShortLinkCreateRespDTO.class);
    }

    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0);
        ShortLinkDO check = baseMapper.selectOne(queryWrapper);
        if(check == null){
            throw new ClientException("修改信息有误");
        }
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag, 0)
                .eq(ShortLinkDO::getEnableStatus, 0)
                .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getValue()), ShortLinkDO::getValidDate, null);
        ShortLinkDO linkDO = ShortLinkDO.builder()
                .domain(check.getDomain())
                .shortUri(check.getShortUri())
                .favicon(check.getFavicon())
                .createdType(check.getCreatedType())
                .originUrl(check.getOriginUrl())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .enableStatus(check.getEnableStatus())
                .shortUri(check.getShortUri())
                .fullShortUrl(requestParam.getFullShortUrl())
                .build();
        if (check.getGid().equals(requestParam.getGid())) {
            baseMapper.update(linkDO, updateWrapper);
        }else{
            check.setDelFlag(1);
            check.setGid(null);
            baseMapper.update(check,queryWrapper);
            linkDO.setGid(requestParam.getGid());
            baseMapper.insert(linkDO);
        }
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);

        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);

        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    @Override
    public ShortLinkGroupRespDTO shortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid,count(*) as shortLinkCount")
                .eq("enable_status", 0)
                .eq("del_flag", 0)
                .in("gid", requestParam)
                .groupBy("gid");
        List<Map<String, Object>> objects = baseMapper.selectMaps(queryWrapper);
        ShortLinkGroupRespDTO shortLinkGroupRespDTO = new ShortLinkGroupRespDTO();
        shortLinkGroupRespDTO.setShortLinkGroupRespDTOS(new HashMap<>());

        for (Map<String, Object> map : objects) {
            shortLinkGroupRespDTO.getShortLinkGroupRespDTOS().put(map.get("gid").toString(), Integer.parseInt(map.get("shortLinkCount").toString()));

        }
        return shortLinkGroupRespDTO;
    }

    private String getShortLink(ShortLinkCreateReqDTO requestParam) {
        String result = "";
        int tryCount = 0;
        while (tryCount<10) {
            result=RandomGenerator.generateRandomString();
            if (!shortLinkCachePenetrationBloomFilter.contains(result)) {
                return result;
            }
            tryCount++;
        }
        throw new ServiceException("短链接注册服务不可用，请稍后再试");

    }
}
