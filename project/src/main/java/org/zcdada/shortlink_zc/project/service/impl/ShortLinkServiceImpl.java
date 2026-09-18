package org.zcdada.shortlink_zc.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
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
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDO;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkGotoDO;
import org.zcdada.shortlink_zc.project.dao.mapper.ShortLinkGotoMapper;
import org.zcdada.shortlink_zc.project.dao.mapper.ShortLinkMapper;
import org.zcdada.shortlink_zc.project.dto.biz.ShortLinkStatsRecordDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkBatchCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkPageReqDTO;
import org.zcdada.shortlink_zc.project.dto.req.ShortLinkUpdateReqDTO;
import org.zcdada.shortlink_zc.project.dto.resp.*;
import org.zcdada.shortlink_zc.project.mq.producer.ShortLinkStatsSaveProducerRabbitMq;
import org.zcdada.shortlink_zc.project.service.ShortLinkService;
import org.zcdada.shortlink_zc.project.service.ShortLinkStatsService;
import org.zcdada.shortlink_zc.project.service.UrlService;
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
    private final ShortLinkStatsService shortLinkStatsService;


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

        shortLinkStatsService.checkGroupBelongToUser(requestParam.getGid());

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
                .favicon(Objects.equals(requestParam.getOriginUrl(), check.getOriginUrl()) ? check.getFavicon() : urlService.getFavicon(requestParam.getOriginUrl()))
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
                .currentDate(new Date())
                .build();
    }
    /**
     * 短链跳转核心方法（访问短链 → 302 跳转到原始长链接）
     *
     * 跳转逻辑分层（由快到慢）：
     *   ① Redis 跳转缓存命中            → 最快，直接跳转（绝大多数请求走这里）
     *   ② 布隆过滤器                    → 拦截不存在的短链（防缓存穿透 / 黑产）
     *   ③ 空值缓存                      → 拦截"查过但不存在 / 已过期"的短链（防恶意重复打库）
     *   ④ 分布式锁单飞（single-flight）→ 缓存未命中时只放一个线程回源数据库，防缓存击穿（惊群）
     */
    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        // ========== 0. 拼装完整短链地址 ==========
        // 例：Host=zcdada.ink、端口=8001、shortUri=vdblft → fullShortLink = "zcdada.ink:8001/vdblft"
        // 注意：这个字符串同时是 Redis 缓存 key 和 DB 里 full_short_url 的组成部分，必须完全一致
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))   // 80 端口是 HTTP 默认，不拼
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortLink = serverName + serverPort + "/" + shortUri;

        // ========== ① 第一层：读跳转缓存（最快路径，99% 的请求在这返回） ==========
        String ori = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortLink));
        if (StrUtil.isNotBlank(ori)) {
            // 缓存命中：说明这个短链存在且未过期 → 记统计 + 302 跳转
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortLink, request, response);
            shortLinkStats(statsRecord);
            ((HttpServletResponse) response).sendRedirect(ori);
            return;
        }

        // ========== ② 布隆过滤器（防缓存穿透 / 黑产） ==========
        // 布隆里"不存在"= 一定不存在（瞎猜的短码/黑产）→ 直接 404，不打数据库
        // 注：布隆有 0.1% 误判率，"存在"不代表真存在，还得继续查
        if (!shortLinkCachePenetrationBloomFilter.contains(fullShortLink)) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }

        // ========== ③ 空值缓存（防恶意重复打库） ==========
        // 之前查过但"不存在/已过期"的短链会写入空值缓存（"-"，5 分钟）→ 命中直接 404
        if (StrUtil.isNotBlank(stringRedisTemplate.opsForValue()
                .get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortLink)))) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }

        // ========== ④ 缓存未命中 → 分布式锁单飞（防缓存击穿） ==========
        // 走到这里说明：缓存没命中、布隆说"可能存在"、无空值缓存 → 可能是个真实短链
        // 若多个请求同时回源数据库会击穿（惊群）→ Redisson 锁只放一个线程查库，其余等待
        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.LOCK_GOTO_SHORT_LINK_KEY, fullShortLink));
        lock.lock();   // 无限等待拿锁：保证拿到锁的线程一定能完成"回源+回填+跳转"（watchdog 30s 兜底）

        try {
            // ========== ⑤ 二次检查缓存（Double-Check） ==========
            // 等锁期间别的线程可能已回填缓存 → 再查一次，命中就直接跳转，省一次 DB 查询
            // ✅ 已移进 try 内：任何 return 路径都会先经过 finally 释放锁（修复锁泄漏 Bug）
            ori = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortLink));
            if (StrUtil.isNotBlank(ori)) {
                ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortLink, request, response);
                shortLinkStats(statsRecord);
                ((HttpServletResponse) response).sendRedirect(ori);
                return;
            }
            // ========== ⑥ 查路由表 t_link_goto（short_uri → gid） ==========
            // 按完整短链地址查"短链 → 分组"索引，定位 gid
            LambdaQueryWrapper<ShortLinkGotoDO> gotoDOLambdaQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortLink);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(gotoDOLambdaQueryWrapper);

            if (shortLinkGotoDO == null || !shortLinkCachePenetrationBloomFilter.contains(fullShortLink)) {
                // 路由表查不到 / 布隆误判 → 非法短链（恶意请求）
                // 风控：写入空值缓存 5 分钟，防止每次都打数据库
                stringRedisTemplate.opsForValue().set(
                        String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortLink), "-", 5, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }

            // ========== ⑦ 查主表 t_link（拿原始链接 + 有效期） ==========
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortLink)
                    .eq(ShortLinkDO::getDelFlag, 0)        // 未删除
                    .eq(ShortLinkDO::getEnableStatus, 0)   // 启用中
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid());
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);

            if (shortLinkDO == null || (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date()))) {
                // 主表查不到 / 已过有效期 → 当作不存在，同样回填空值缓存 5 分钟
                stringRedisTemplate.opsForValue().set(
                        String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortLink), "-", 5, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }

            // ========== ⑧ 回填跳转缓存 ==========
            // 查库成功 → 写回 Redis（key=完整短链地址，value=原始长链接）
            // TTL：永久链接=DEFAULT_CACHE_TIME(2628000ms≈43.8分钟)，有有效期=到有效期为止
            // ⚠️ 43.8 分钟意味着"永久"短链缓存也会中途过期 → 过期瞬间触发回源（压测 48 超时的诱因）
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, shortLinkDO.getFullShortUrl()),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),
                    TimeUnit.MILLISECONDS);

            // ========== ⑨ 记统计 + 302 跳转 ==========
            // ⚠️ 统计发送在锁内且同步：若 RabbitMQ 流控，会卡住持锁线程（第二个隐患，建议移到锁外）
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortLink, request, response);
            shortLinkStats(statsRecord);
            ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());

        } finally {
            lock.unlock();   // ========== ⑩ 释放锁（保护 ⑤⑥⑦⑧⑨：双重检查 + 回源逻辑，必然执行） ==========
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
