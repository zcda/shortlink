package org.zcdada.shortlink_zc.project.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zcdada.shortlink_zc.project.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.project.dao.entity.*;
import org.zcdada.shortlink_zc.project.dao.mapper.*;
import org.zcdada.shortlink_zc.project.dto.biz.ShortLinkStatsRecordDTO;
import org.zcdada.shortlink_zc.project.service.ShortLinkStatsPersistenceService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.zcdada.shortlink_zc.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static org.zcdada.shortlink_zc.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

@Service
@RequiredArgsConstructor
public class ShortLinkStatsPersistenceServiceImpl implements ShortLinkStatsPersistenceService {

    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final RedissonClient redissonClient;
    private final ShortLinkAccessStatsMapper shortLinkAccessStatsMapper;
    private final ShortLinkLocaleStatsMapper shortLinkLocaleStatsMapper;
    private final ShortLinkOsStatsMapper shortLinkOsStatsMapper;
    private final ShortLinkBrowserStatsMapper shortLinkBrowserStatsMapper;
    private final ShortLinkDeviceStatsMapper shortLinkDeviceStatsMapper;
    private final ShortLinkNetworkStatsMapper shortLinkNetworkStatsMapper;
    private final ShortLinkAccessLogsMapper shortLinkAccessLogsMapper;
    private final ShortLinkStatsTodayMapper shortLinkStatsTodayMapper;
    private final ShortLinkMapper shortLinkMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${short-link.stats.locale.amap-key:}")
    private String amapKey;

    @Value("${short-link.stats.locale.enabled:true}")
    private boolean localeEnabled;

    @Value("${short-link.stats.locale.cache-enabled:true}")
    private boolean localeCacheEnabled;

    @Value("${short-link.stats.locale.cache-ttl-seconds:86400}")
    private long localeCacheTtlSeconds;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(ShortLinkStatsRecordDTO statsRecord) {
        String fullShortUrl = statsRecord.getFullShortUrl();
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        try {
            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
            if (shortLinkGotoDO == null) {
                throw new ClientException("短链接不存在");
            }
            String gid = shortLinkGotoDO.getGid();

            Date date = statsRecord.getCurrentDate() == null ? new Date() : statsRecord.getCurrentDate();
            int hour = DateUtil.hour(date, true);
            Week week = DateUtil.dayOfWeekEnum(date);
            JSONObject localeJsonObject = resolveLocale(statsRecord.getRemoteAddr());

            shortLinkAccessStatsMapper.shortLinkStats(ShortLinkAccessStatsDO.builder()
                    .fullShortUrl(fullShortUrl).pv(1)
                    .uv(Boolean.TRUE.equals(statsRecord.getUvFirstFlag()) ? 1 : 0)
                    .uip(Boolean.TRUE.equals(statsRecord.getUipFirstFlag()) ? 1 : 0)
                    .date(date).hour(hour).weekday(week.getIso8601Value()).build());

            if (localeJsonObject != null && "10000".equals(localeJsonObject.getString("infocode"))) {
                String province = localeJsonObject.getString("province");
                boolean unknown = StringUtil.equals(province, "[]");
                shortLinkLocaleStatsMapper.shortLinkLocaleStats(ShortLinkLocaleStatsDO.builder()
                        .fullShortUrl(fullShortUrl).cnt(1)
                        .province(unknown ? "未知" : province)
                        .city(unknown ? "未知" : localeJsonObject.getString("city"))
                        .adcode(unknown ? "未知" : localeJsonObject.getString("adcode"))
                        .country("中国").date(date).build());
            }

            shortLinkOsStatsMapper.shortLinkOsStats(ShortLinkOsStatsDO.builder()
                    .fullShortUrl(fullShortUrl).cnt(1).os(statsRecord.getOs()).date(date).build());
            shortLinkBrowserStatsMapper.shortLinkBrowserStats(ShortLinkBrowserStatsDO.builder()
                    .browser(statsRecord.getBrowser()).fullShortUrl(fullShortUrl).cnt(1).date(date).build());
            shortLinkDeviceStatsMapper.shortLinkDeviceStats(ShortLinkDeviceStatsDO.builder()
                    .fullShortUrl(fullShortUrl).cnt(1).device(statsRecord.getDevice()).date(date).build());
            shortLinkNetworkStatsMapper.shortLinkNetworkStats(ShortLinkNetworkStatsDO.builder()
                    .fullShortUrl(fullShortUrl).cnt(1).network(statsRecord.getNetwork()).date(date).build());

            shortLinkAccessLogsMapper.insert(ShortLinkAccessLogsDO.builder()
                    .fullShortUrl(fullShortUrl).ip(statsRecord.getRemoteAddr()).user(statsRecord.getUv())
                    .browser(statsRecord.getBrowser()).os(statsRecord.getOs()).device(statsRecord.getDevice())
                    .locale(localeJsonObject == null ? null : localeJsonObject.getString("province"))
                    .network(statsRecord.getNetwork()).build());

            shortLinkMapper.incrementStats(gid, fullShortUrl, 1,
                    Boolean.TRUE.equals(statsRecord.getUvFirstFlag()) ? 1 : 0,
                    Boolean.TRUE.equals(statsRecord.getUipFirstFlag()) ? 1 : 0);
            shortLinkStatsTodayMapper.shortLinkTodayState(ShortLinkStatsTodayDO.builder()
                    .todayPv(1).todayUv(Boolean.TRUE.equals(statsRecord.getUvFirstFlag()) ? 1 : 0)
                    .todayUip(Boolean.TRUE.equals(statsRecord.getUipFirstFlag()) ? 1 : 0)
                    .fullShortUrl(fullShortUrl).date(date).build());
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("短链接访问统计异常");
        } finally {
            rLock.unlock();
        }
    }

    private JSONObject resolveLocale(String remoteAddr) {
        if (!localeEnabled || StrUtil.isBlank(remoteAddr) || StrUtil.isBlank(amapKey)) {
            return null;
        }
        String cacheKey = "short-link:stats:locale:" + remoteAddr;
        try {
            if (localeCacheEnabled) {
                String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (StrUtil.isNotBlank(cached)) {
                    return JSON.parseObject(cached);
                }
            }
            Map<String, Object> params = new HashMap<>();
            params.put("key", amapKey);
            params.put("ip", remoteAddr);
            JSONObject result = JSON.parseObject(HttpUtil.get(AMAP_REMOTE_URL, params));
            if (localeCacheEnabled && result != null && "10000".equals(result.getString("infocode"))) {
                stringRedisTemplate.opsForValue().set(cacheKey, result.toJSONString(),
                        localeCacheTtlSeconds, java.util.concurrent.TimeUnit.SECONDS);
            }
            return result;
        } catch (Exception ignored) {
            // 地区解析失败不应阻断其它统计数据落库。
            return null;
        }
    }
}
