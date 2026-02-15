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
import groovy.util.logging.Slf4j;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.project.common.constant.RedisKeyConstant;
import org.zcdada.shortlink_zc.project.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.project.common.convention.exception.ServiceException;
import org.zcdada.shortlink_zc.project.common.enums.VailDateTypeEnum;
import org.zcdada.shortlink_zc.project.dao.entity.*;
import org.zcdada.shortlink_zc.project.dao.mapper.*;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkPageReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkUpdateReqDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkCreateRespDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkGroupRespDTO;
import org.zcdada.shortlink_zc.project.dto.resp.ShortLinkPageRespDTO;
import org.zcdada.shortlink_zc.project.service.ShortLinkService;
import org.zcdada.shortlink_zc.project.service.UrlService;
import org.zcdada.shortlink_zc.project.toolkit.IpUtils;
import org.zcdada.shortlink_zc.project.toolkit.LinkUtil;
import org.zcdada.shortlink_zc.project.toolkit.RandomGenerator;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.zcdada.shortlink_zc.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

@Slf4j
@RequiredArgsConstructor
@Service
public class ShortLinkServiceImpl  extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortLinkCachePenetrationBloomFilter;

    private final ShortLinkGotoMapper shortLinkGotoMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    private final UrlService urlService;

    private final ShortLinkAccessStatsMapper shortLinkAccessStatsMapper;
    private final ShortLinkLocaleStatsMapper shortLinkLocaleStatsMapper;
    private final ShortLinkOsStatsMapper shortLinkOsStatsMapper;
    private final ShortLinkBrowserStatsMapper shortLinkBrowserStatsMapper;
    private final ShortLinkDeviceStatsMapper shortLinkDeviceStatsMapper;
    private final ShortLinkNetworkStatsMapper shortLinkNetworkStatsMapper;
    private final ShortLinkAccessLogsMapper shortLinkAccessLogsMapper;
    private final ShortLinkStatsTodayMapper shortLinkStatsTodayMapper;


    @Value("${short-link.stats.locale.amap-key}")
    private String amapKey;

    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        // todo 检查是否为当前用户的gid且存在
        String shortLinkUri = getShortLink(requestParam);
        String fullShortUrl=requestParam.getDomain() + "/" + shortLinkUri;
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
                .fullShortUrl(fullShortUrl)
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
            throw new ServiceException("重复短链接,请稍后再试试");
        }
        stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,
                fullShortUrl),
                shortLinkDO.getOriginUrl(),
                LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                TimeUnit.MILLISECONDS
        );

        shortLinkCachePenetrationBloomFilter.add(fullShortUrl);
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
            baseMapper.delete(queryWrapper);
            linkDO.setGid(requestParam.getGid());

            ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder().
                    gid(requestParam.getGid()).
                    fullShortUrl(requestParam.getFullShortUrl()).
                    build();
            shortLinkGotoMapper.insert(shortLinkGotoDO);
            baseMapper.insert(linkDO);
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

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        String serverName = request.getServerName();
        String fullShortLink = serverName+"/" + shortUri;

        String ori = stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,fullShortLink));
        if (StrUtil.isNotBlank(ori)) {
            shortLinkStats(fullShortLink, request, response);
            ((HttpServletResponse)response).sendRedirect(ori);
            return;
        }

        if (!shortLinkCachePenetrationBloomFilter.contains(fullShortLink)) {
            ((HttpServletResponse)response).sendRedirect("/page/notfound");
            return;
        }

        String s = stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstant.GOTO_NULL_LINK_KEY, fullShortLink));
        if (StrUtil.isNotBlank(s)) {
            ((HttpServletResponse)response).sendRedirect("/page/notfound");
            return;
        }


        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_GOTO_SHORT_LINK_KEY,fullShortLink));
        lock.lock();

        ori = stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,fullShortLink));
        if (StrUtil.isNotBlank(ori)) {
            shortLinkStats(fullShortLink, request, response);
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
                stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_NULL_LINK_KEY,fullShortLink),"-",5, TimeUnit.MINUTES);
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
                    stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_NULL_LINK_KEY,fullShortLink),"-",5, TimeUnit.MINUTES);
                    ((HttpServletResponse)response).sendRedirect("/page/notfound");
                    return;
            }


            stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY,
                                shortLinkDO.getFullShortUrl()),
                        shortLinkDO.getOriginUrl(),
                        LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                        TimeUnit.MILLISECONDS
                );
            shortLinkStats(fullShortLink, request, response);
            ((HttpServletResponse)response).sendRedirect(shortLinkDO.getOriginUrl());


        }finally {
            lock.unlock();
        }
    }

    /**
     * @Author: zcdada
     * @Description: 用于监控短链接跳转的信息
     * @DateTime: 2025/12/11 17:09
     */
    private void shortLinkStats(String fullShortLink,ServletRequest request, ServletResponse response){

        //通过cookie判断当前用户是否为老用户
        try{
            Cookie[] cookies = ((HttpServletRequest) request).getCookies();
            AtomicBoolean uvFlag = new AtomicBoolean(false);


            AtomicReference<String> realUv = new AtomicReference<>();


            Runnable run = ()->{
                String uv = UUID.randomUUID().toString();
                Cookie uvCookie = new Cookie("uv", uv);
                uvCookie.setMaxAge(60 * 60 * 24 * 30);
                uvCookie.setPath(StrUtil.sub(fullShortLink,fullShortLink.indexOf("/"),fullShortLink.length()));
                ((HttpServletResponse)response).addCookie(uvCookie);
                uvFlag.set(true);
                realUv.set(uv);
                stringRedisTemplate.opsForSet().add(String.format(RedisKeyConstant.SHORT_LINK_STATS_UV_KEY, fullShortLink), uv);
            };

            if (ArrayUtil.isNotEmpty(cookies)) {
                Arrays.stream(cookies)
                        .filter(each ->Objects.equals(each.getName(),"uv"))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(uv -> {
                            realUv.set(uv);
                            Long uvAdd = stringRedisTemplate.opsForSet().add(String.format(RedisKeyConstant.SHORT_LINK_STATS_UV_KEY, fullShortLink), uv);
                            uvFlag.set(uvAdd != null && uvAdd > 0L);
                        },run);
            }else{
                run.run();
            }

            boolean uipFlag = false;
            String remoteAddr = IpUtils.getClientRealIp((HttpServletRequest) request);
            Long uvAdd = stringRedisTemplate.opsForSet().add(String.format(RedisKeyConstant.SHORT_LINK_STATS_UIP_KEY, fullShortLink),remoteAddr) ;
            uipFlag=uvAdd != null && uvAdd > 0L;

            Date date = new Date();
            int hour = DateUtil.hour(date, true);
            Week week = DateUtil.dayOfWeekEnum(date);

            ShortLinkAccessStatsDO shortLinkAccessStatsDO = ShortLinkAccessStatsDO.builder()
                    .fullShortUrl(fullShortLink)
                    .pv(1)
                    .uv(uvFlag.get()?1:0)
                    .uip(uipFlag?1:0)
                    .date(date)
                    .hour(hour)
                    .weekday(week.getIso8601Value())
                    .build();
            shortLinkAccessStatsMapper.shortLinkStats(shortLinkAccessStatsDO);


            Map<String,Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key",amapKey);
            localeParamMap.put("ip",remoteAddr);
            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeParamMap);
            JSONObject localeJsonObject = JSON.parseObject(localeResultStr);
            String infoCode = localeJsonObject.getString("infocode");
            ShortLinkLocaleStatsDO shortLinkLocaleStatsDO;
            if (StrUtil.isNotEmpty(infoCode)&&StrUtil.equals(infoCode,"10000")) {
                String province = localeJsonObject.getString("province");
                boolean unknownFlag = StringUtil.equals(province,"[]");

                shortLinkLocaleStatsDO=ShortLinkLocaleStatsDO.builder()
                        .fullShortUrl(fullShortLink)
                        .cnt(1)
                        .province(unknownFlag?"未知":province)
                        .city(unknownFlag?"未知": localeJsonObject.getString("city"))
                        .adcode(unknownFlag?"未知":localeJsonObject.getString("adcode"))
                        .country("中国")
                        .date(date)
                        .build();
                shortLinkLocaleStatsMapper.shortLinkLocaleStats(shortLinkLocaleStatsDO);
            }

            String operatingSystem = LinkUtil.getOperatingSystem((HttpServletRequest) request);
            ShortLinkOsStatsDO shortLinkOsStatsDO = ShortLinkOsStatsDO.builder()
                    .fullShortUrl(fullShortLink)
                    .cnt(1)
                    .os(operatingSystem)
                    .date(date)
                    .build();

            shortLinkOsStatsMapper.shortLinkOsStats(shortLinkOsStatsDO);


            String browser = LinkUtil.getBrowser((HttpServletRequest) request);
            ShortLinkBrowserStatsDO shortLinkBrowserStatsDO = ShortLinkBrowserStatsDO.builder()
                    .browser(browser)
                    .fullShortUrl(fullShortLink)
                    .cnt(1)
                    .date(date)
                    .build();
            shortLinkBrowserStatsMapper.shortLinkBrowserStats(shortLinkBrowserStatsDO);

            String device = LinkUtil.getDevice((HttpServletRequest) request);
            ShortLinkDeviceStatsDO shortLinkDeviceStatsDO = ShortLinkDeviceStatsDO.builder()
                    .fullShortUrl(fullShortLink)
                    .cnt(1)
                    .device(device)
                    .date(date)
                    .build();
            shortLinkDeviceStatsMapper.shortLinkDeviceStats(shortLinkDeviceStatsDO);

            String network = LinkUtil.getNetwork((HttpServletRequest) request);
            ShortLinkNetworkStatsDO shortLinkNetworkStatsDO = ShortLinkNetworkStatsDO.builder()
                    .fullShortUrl(fullShortLink)
                    .cnt(1)
                    .network(network)
                    .date(date)
                    .build();
            shortLinkNetworkStatsMapper.shortLinkNetworkStats(shortLinkNetworkStatsDO);


            ShortLinkAccessLogsDO shortLinkAccessLogsDO = ShortLinkAccessLogsDO.builder()
                    .fullShortUrl(fullShortLink)
                    .ip(remoteAddr)
                    .user(realUv.get())
                    .locale(localeJsonObject.getString("province"))
                    .browser(browser)
                    .device(device)
                    .os(operatingSystem)
                    .network(network)
                    .build();
            shortLinkAccessLogsMapper.insert(shortLinkAccessLogsDO);

            baseMapper.incrementStats(fullShortLink, 1, uvFlag.get()?1:0, uipFlag?1:0);

            ShortLinkStatsTodayDO linkStatsTodayDO = ShortLinkStatsTodayDO.builder()
                    .todayPv(1)
                    .todayUv(uvFlag.get()?1:0)
                    .todayUip(uipFlag?1:0)
                    .fullShortUrl(fullShortLink)
                    .date(new Date())
                    .build();
            shortLinkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);

        }catch (Exception e){
            throw new ClientException("短链接访问统计异常");
        }


    }

    private String getShortLink(ShortLinkCreateReqDTO requestParam) {
        String result = "";
        int tryCount = 0;
        while (tryCount<10) {
            result=RandomGenerator.generateRandomString();
            if (!shortLinkCachePenetrationBloomFilter.contains(requestParam.getDomain()+"/"+result)) {
                return result;
            }
            tryCount++;
        }
        throw new ServiceException("短链接注册服务不可用，请稍后再试");

    }
}
