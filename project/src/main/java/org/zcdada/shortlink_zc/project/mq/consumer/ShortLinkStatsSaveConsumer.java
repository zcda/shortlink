
package org.zcdada.shortlink_zc.project.mq.consumer;

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
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import org.zcdada.shortlink_zc.project.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.project.config.GotoDomainWhiteListConfiguration;
import org.zcdada.shortlink_zc.project.dao.entity.*;
import org.zcdada.shortlink_zc.project.dao.mapper.*;
import org.zcdada.shortlink_zc.project.dto.biz.ShortLinkStatsRecordDTO;
import org.zcdada.shortlink_zc.project.mq.producer.DelayShortLinkStatsProducer;
import org.zcdada.shortlink_zc.project.mq.producer.ShortLinkStatsSaveProducer;
import org.zcdada.shortlink_zc.project.service.LinkStatsTodayService;
import org.zcdada.shortlink_zc.project.service.UrlService;

import java.util.*;

import static org.zcdada.shortlink_zc.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static org.zcdada.shortlink_zc.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;


/**
 * 短链接监控状态保存消息队列消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveConsumer implements StreamListener<String, MapRecord<String, String, String>> {

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
    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;
    private final LinkStatsTodayService linkStatsTodayService;
    private final ShortLinkMapper shortLinkMapper;
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;
    private final DelayShortLinkStatsProducer delayShortLinkStatsProducer;

    @Value("${short-link.stats.locale.amap-key}")
    private String amapKey;


    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String stream = message.getStream();
        RecordId id = message.getId();
        Map<String, String> producerMap = message.getValue();
        String fullShortUrl = producerMap.get("fullShortUrl");
        if (StrUtil.isNotBlank(fullShortUrl)) {
            String gid = producerMap.get("gid");
            ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(producerMap.get("statsRecord"), ShortLinkStatsRecordDTO.class);
            actualSaveShortLinkStats(fullShortUrl, gid, statsRecord);
        }
        stringRedisTemplate.opsForStream().delete(Objects.requireNonNull(stream), id.getValue());
    }

    public void actualSaveShortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord) {
        fullShortUrl = Optional.ofNullable(fullShortUrl).orElse(statsRecord.getFullShortUrl());
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        if (!rLock.tryLock()) {
            delayShortLinkStatsProducer.send(statsRecord);
            return;
        }
        try {
            //通过cookie判断当前用户是否为老用户

            if (StrUtil.isBlank(gid)) {
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, statsRecord.getFullShortUrl());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }


            Date date = new Date();
            int hour = DateUtil.hour(date, true);
            Week week = DateUtil.dayOfWeekEnum(date);

            ShortLinkAccessStatsDO shortLinkAccessStatsDO = ShortLinkAccessStatsDO.builder()
                    .fullShortUrl(statsRecord.getFullShortUrl())
                    .pv(1)
                    .uv(statsRecord.getUvFirstFlag() ? 1 : 0)
                    .uip(statsRecord.getUipFirstFlag() ? 1 : 0)
                    .date(date)
                    .hour(hour)
                    .weekday(week.getIso8601Value())
                    .build();
            shortLinkAccessStatsMapper.shortLinkStats(shortLinkAccessStatsDO);


            Map<String,Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key",amapKey);
            localeParamMap.put("ip",statsRecord.getRemoteAddr());
            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeParamMap);
            JSONObject localeJsonObject = JSON.parseObject(localeResultStr);
            String infoCode = localeJsonObject.getString("infocode");
            ShortLinkLocaleStatsDO shortLinkLocaleStatsDO;
            if (StrUtil.isNotEmpty(infoCode)&&StrUtil.equals(infoCode,"10000")) {
                String province = localeJsonObject.getString("province");
                boolean unknownFlag = StringUtil.equals(province,"[]");

                shortLinkLocaleStatsDO=ShortLinkLocaleStatsDO.builder()
                        .fullShortUrl(fullShortUrl)
                        .cnt(1)
                        .province(unknownFlag?"未知":province)
                        .city(unknownFlag?"未知": localeJsonObject.getString("city"))
                        .adcode(unknownFlag?"未知":localeJsonObject.getString("adcode"))
                        .country("中国")
                        .date(date)
                        .build();
                shortLinkLocaleStatsMapper.shortLinkLocaleStats(shortLinkLocaleStatsDO);
            }


            ShortLinkOsStatsDO shortLinkOsStatsDO = ShortLinkOsStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .cnt(1)
                    .os(statsRecord.getOs())
                    .date(date)
                    .build();

            shortLinkOsStatsMapper.shortLinkOsStats(shortLinkOsStatsDO);



            ShortLinkBrowserStatsDO shortLinkBrowserStatsDO = ShortLinkBrowserStatsDO.builder()
                    .browser(statsRecord.getBrowser())
                    .fullShortUrl(fullShortUrl)
                    .cnt(1)
                    .date(date)
                    .build();
            shortLinkBrowserStatsMapper.shortLinkBrowserStats(shortLinkBrowserStatsDO);


            ShortLinkDeviceStatsDO shortLinkDeviceStatsDO = ShortLinkDeviceStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .cnt(1)
                    .device(statsRecord.getDevice())
                    .date(date)
                    .build();
            shortLinkDeviceStatsMapper.shortLinkDeviceStats(shortLinkDeviceStatsDO);


            ShortLinkNetworkStatsDO shortLinkNetworkStatsDO = ShortLinkNetworkStatsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .cnt(1)
                    .network(statsRecord.getNetwork())
                    .date(date)
                    .build();
            shortLinkNetworkStatsMapper.shortLinkNetworkStats(shortLinkNetworkStatsDO);


            ShortLinkAccessLogsDO shortLinkAccessLogsDO = ShortLinkAccessLogsDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .ip(statsRecord.getRemoteAddr())
                    .user(statsRecord.getUv())
                    .ip(statsRecord.getRemoteAddr())
                    .browser(statsRecord.getBrowser())
                    .os(statsRecord.getOs())
                    .device(statsRecord.getDevice())
                    .locale(localeJsonObject.getString("province"))
                    .network(statsRecord.getNetwork())
                    .build();
            shortLinkAccessLogsMapper.insert(shortLinkAccessLogsDO);

            shortLinkMapper.incrementStats(gid,fullShortUrl, 1, statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0);

            ShortLinkStatsTodayDO linkStatsTodayDO = ShortLinkStatsTodayDO.builder()
                    .todayPv(1)
                    .todayUv(statsRecord.getUvFirstFlag() ? 1 : 0)
                    .todayUip(statsRecord.getUipFirstFlag() ? 1 : 0)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            shortLinkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);

        }catch (Exception e){
            throw new ClientException("短链接访问统计异常");
        }finally {
            rLock.unlock();
        }
    }
}
