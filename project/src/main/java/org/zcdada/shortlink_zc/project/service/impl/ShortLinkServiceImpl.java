package org.zcdada.shortlink_zc.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zcdada.shortlink_zc.project.common.constant.RedisKeyConstant;
import org.zcdada.shortlink_zc.project.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.project.common.convention.exception.ServiceException;
import org.zcdada.shortlink_zc.project.common.enums.VailDateTypeEnum;
import org.zcdada.shortlink_zc.project.config.GotoDomainWhiteListConfiguration;
import org.zcdada.shortlink_zc.project.dao.entity.*;
import org.zcdada.shortlink_zc.project.dao.mapper.*;
import org.zcdada.shortlink_zc.project.dto.biz.ShortLinkStatsRecordDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkBatchCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkPageReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkUpdateReqDTO;
import org.zcdada.shortlink_zc.project.dto.resp.*;
import org.zcdada.shortlink_zc.project.mq.producer.DelayShortLinkStatsProducer;
import org.zcdada.shortlink_zc.project.mq.producer.ShortLinkStatsSaveProducer;
import org.zcdada.shortlink_zc.project.mq.producer.ShortLinkStatsSaveProducerRabbitMq;
import org.zcdada.shortlink_zc.project.service.LinkStatsTodayService;
import org.zcdada.shortlink_zc.project.service.ShortLinkService;
import org.zcdada.shortlink_zc.project.service.UrlService;
import org.zcdada.shortlink_zc.project.toolkit.IpUtils;
import org.zcdada.shortlink_zc.project.toolkit.LinkUtil;
import org.zcdada.shortlink_zc.project.toolkit.RandomGenerator;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.zcdada.shortlink_zc.project.common.constant.RedisKeyConstant.*;


@Slf4j
@RequiredArgsConstructor
@Service
public class ShortLinkServiceImpl  extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortLinkCachePenetrationBloomFilter;

    private final ShortLinkGotoMapper shortLinkGotoMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    private final UrlService urlService;


    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;
    private final ShortLinkStatsSaveProducerRabbitMq shortLinkStatsSaveProducer;


    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;


    private void verificationWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        if (enable == null || !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("跳转链接填写错误");
        }
        List<String> details = gotoDomainWhiteListConfiguration.getDetails();
        if (!details.contains(domain)) {
            throw new ClientException("演示环境为避免恶意攻击，请生成以下网站跳转链接：" + gotoDomainWhiteListConfiguration.getNames());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        verificationWhitelist(requestParam.getOriginUrl());

        // todo 检查是否为当前用户的gid且存在
        String shortLinkUri = getShortLink(requestParam);
        String fullShortUrl=createShortLinkDefaultDomain + "/" + shortLinkUri;
        ShortLinkDO shortLinkDO = new ShortLinkDO().builder()
                .gid(requestParam.getGid())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .createdType(requestParam.getCreatedType())
                .domain(createShortLinkDefaultDomain)
                .enableStatus(0)
                .originUrl(requestParam.getOriginUrl())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .shortUri(shortLinkUri)
                .fullShortUrl(fullShortUrl)
                .delTime(0L)
                .favicon(urlService.getFavicon(requestParam.getOriginUrl()))
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .build();

        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder().
                gid(requestParam.getGid()).
                fullShortUrl(fullShortUrl).
                build();

        try{
            baseMapper.insert(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        }catch (Exception e){
            log.warn("重复短链接");
            System.out.println(e.getMessage());
            //并发情况下有极低概率多个相同fullShortUrl打入数据库,但是还没更新缓存shortLinkCachePenetrationBloomFilter
            if(!shortLinkCachePenetrationBloomFilter.contains(fullShortUrl)){
                shortLinkCachePenetrationBloomFilter.add(fullShortUrl);
            }
            throw new ServiceException("重复短链接,请稍后再试试");
        }
        stringRedisTemplate.opsForValue().set(String.format(GOTO_SHORT_LINK_KEY,
                fullShortUrl),
                shortLinkDO.getOriginUrl(),
                LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                TimeUnit.MILLISECONDS
        );

        shortLinkCachePenetrationBloomFilter.add(fullShortUrl);
        return BeanUtil.toBean(shortLinkDO, ShortLinkCreateRespDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        verificationWhitelist(requestParam.getOriginUrl());


        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParam.getOriginGid())
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
                .set(Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);

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
            //如果分组没有改变
            baseMapper.update(linkDO, updateWrapper);
        }else{
            //如果分组改变
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, requestParam.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            if (!rLock.tryLock()) {
                throw new ServiceException("短链接正在被访问，请稍后再试...");
            }
            try {
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, check.getGid())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
                        .delTime(System.currentTimeMillis())
                        .build();
                delShortLinkDO.setDelFlag(1);
                baseMapper.update(delShortLinkDO, linkUpdateWrapper);
                ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                        .domain(createShortLinkDefaultDomain)
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(check.getCreatedType())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .describe(requestParam.getDescribe())
                        .shortUri(check.getShortUri())
                        .enableStatus(check.getEnableStatus())
                        .totalPv(check.getTotalPv())
                        .totalUv(check.getTotalUv())
                        .totalUip(check.getTotalUip())
                        .fullShortUrl(check.getFullShortUrl())
                        .favicon(check.getFavicon())
                        .delTime(0L)
                        .build();
                baseMapper.insert(shortLinkDO);

//                ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder().
//                        gid(requestParam.getGid()).
//                        fullShortUrl(requestParam.getFullShortUrl()).
//                        build();
//                shortLinkGotoMapper.insert(shortLinkGotoDO);

                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, check.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                shortLinkGotoMapper.deleteById(shortLinkGotoDO.getId());
                shortLinkGotoDO.setGid(requestParam.getGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);

            } finally {
                rLock.unlock();
            }
        }

        //如果当前短链接有 getValidDateType的修改 或者 过期时间有修改 则删除缓存
        if (!Objects.equals(check.getValidDateType(), requestParam.getValidDateType())
                || !Objects.equals(check.getValidDate(), requestParam.getValidDate())
                || !Objects.equals(check.getOriginUrl(), requestParam.getOriginUrl())) {
            stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
            if (check.getValidDate() != null && check.getValidDate().before(new Date())) {
                if (Objects.equals(requestParam.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()) || requestParam.getValidDate().after(new Date())) {
                    stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                }
            }
        }
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        IPage<ShortLinkDO> resultPage = baseMapper.pageLink(requestParam);


        return resultPage.convert(each -> {
            each.setDomain("http://" + each.getDomain());
            return BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
        });
    }

    @Override
    public ShortLinkGroupRespDTO shortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid,count(*) as shortLinkCount")
                .eq("enable_status", 0)
                .eq("del_flag", 0)
                .eq("del_time", 0L)
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

    private ShortLinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        AtomicReference<String> uv = new AtomicReference<>();
        Runnable addResponseCookieTask = () -> {
            uv.set(cn.hutool.core.lang.UUID.fastUUID().toString());
            Cookie uvCookie = new Cookie("uv", uv.get());
            uvCookie.setMaxAge(60 * 60 * 24 * 30);
            uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
            ((HttpServletResponse) response).addCookie(uvCookie);
            uvFirstFlag.set(Boolean.TRUE);
            stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, uv.get());
        };
        if (ArrayUtil.isNotEmpty(cookies)) {
            Arrays.stream(cookies)
                    .filter(each -> Objects.equals(each.getName(), "uv"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .ifPresentOrElse(each -> {
                        uv.set(each);
                        Long uvAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, each);
                        uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                    }, addResponseCookieTask);
        } else {
            addResponseCookieTask.run();
        }
        String remoteAddr = LinkUtil.getClientRealIp(((HttpServletRequest) request));
        String os = LinkUtil.getOperatingSystem(((HttpServletRequest) request));
        String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
        String device = LinkUtil.getDevice(((HttpServletRequest) request));
        String network = LinkUtil.getNetwork(((HttpServletRequest) request));
        Long uipAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UIP_KEY + fullShortUrl, remoteAddr);
        boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
        return ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv.get())
                .uvFirstFlag(uvFirstFlag.get())
                .uipFirstFlag(uipFirstFlag)
                .remoteAddr(remoteAddr)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .build();
    }
    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        String serverName = request.getServerName();

        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");

        String fullShortLink = serverName+serverPort+"/" + shortUri;

        String ori = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY,fullShortLink));
        if (StrUtil.isNotBlank(ori)) {
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortLink, request, response);
            shortLinkStats(statsRecord);
            ((HttpServletResponse)response).sendRedirect(ori);
            return;
        }

        if (!shortLinkCachePenetrationBloomFilter.contains(fullShortLink)) {
            ((HttpServletResponse)response).sendRedirect("/page/notfound");
            return;
        }

        String s = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortLink));
        if (StrUtil.isNotBlank(s)) {
            ((HttpServletResponse)response).sendRedirect("/page/notfound");
            return;
        }


        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_GOTO_SHORT_LINK_KEY,fullShortLink));
        lock.lock();

        ori = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY,fullShortLink));
        if (StrUtil.isNotBlank(ori)) {
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortLink, request, response);
            shortLinkStats(statsRecord);
            ((HttpServletResponse)response).sendRedirect(ori);
            return;
        }

        try{

            LambdaQueryWrapper<ShortLinkGotoDO> gotoDOLambdaQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortLink);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(gotoDOLambdaQueryWrapper);

            if(shortLinkGotoDO==null||!shortLinkCachePenetrationBloomFilter.contains(fullShortLink)){
                //做风控 可能是有人恶意请求错误短链接
                // 把错误的也放到缓存中，防止一直访问数据库
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,fullShortLink),"-",5, TimeUnit.MINUTES);
                ((HttpServletResponse)response).sendRedirect("/page/notfound");
                return;
            }

            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortLink)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid());
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);

            if (shortLinkDO==null||(shortLinkDO.getValidDate()!=null&&shortLinkDO.getValidDate().before(new Date()))){
                    //数据过期
                    stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,fullShortLink),"-",5, TimeUnit.MINUTES);
                    ((HttpServletResponse)response).sendRedirect("/page/notfound");
                    return;
            }


            stringRedisTemplate.opsForValue().set(String.format(GOTO_SHORT_LINK_KEY,
                                shortLinkDO.getFullShortUrl()),
                        shortLinkDO.getOriginUrl(),
                        LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                        TimeUnit.MILLISECONDS
                );
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortLink, request, response);
            shortLinkStats(statsRecord);
            ((HttpServletResponse)response).sendRedirect(shortLinkDO.getOriginUrl());


        }finally {
            lock.unlock();
        }
    }

    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            ShortLinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                ShortLinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);
                ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Throwable ex) {
                log.error("批量创建短链接失败，原始参数：{}", originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }

    /**
     * @Author: zcdada
     * @Description: 用于监控短链接跳转的信息
     * @DateTime: 2025/12/11 17:09
     */
    @Override
    public void shortLinkStats(ShortLinkStatsRecordDTO statsRecord){

        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(producerMap);

    }

    private String getShortLink(ShortLinkCreateReqDTO requestParam) {
        String result = "";
        int tryCount = 0;
        while (tryCount<10) {
            result=RandomGenerator.generateRandomString();
            if (!shortLinkCachePenetrationBloomFilter.contains(createShortLinkDefaultDomain+"/"+result)) {
                return result;
            }
            tryCount++;
        }
        throw new ServiceException("短链接注册服务不可用，请稍后再试");

    }
}
