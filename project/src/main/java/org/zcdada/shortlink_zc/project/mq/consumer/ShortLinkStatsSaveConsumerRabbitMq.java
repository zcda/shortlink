package org.zcdada.shortlink_zc.project.mq.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.zcdada.shortlink_zc.project.common.convention.exception.ClientException;
import org.zcdada.shortlink_zc.project.config.GotoDomainWhiteListConfiguration;
import org.zcdada.shortlink_zc.project.dao.entity.*;
import org.zcdada.shortlink_zc.project.dao.mapper.*;
import org.zcdada.shortlink_zc.project.dto.biz.ShortLinkStatsRecordDTO;
import org.zcdada.shortlink_zc.project.mq.idempotent.MessageQueueIdempotentHandler;
import org.zcdada.shortlink_zc.project.mq.producer.DelayShortLinkStatsProducer;
import org.zcdada.shortlink_zc.project.mq.producer.ShortLinkStatsSaveProducer;
import org.zcdada.shortlink_zc.project.service.LinkStatsTodayService;
import org.zcdada.shortlink_zc.project.service.UrlService;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.zcdada.shortlink_zc.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static org.zcdada.shortlink_zc.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;
import static org.zcdada.shortlink_zc.project.mq.config.RabbitMQConfig.STATS_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveConsumerRabbitMq {
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;
    // 其他依赖注入保持不变（shortLinkMapper、shortLinkAccessStatsMapper 等）
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
    /**
     * 监听 RabbitMQ 队列，手动确认模式
     */
    @RabbitListener(queues = STATS_QUEUE, ackMode = "MANUAL")
    public void onMessage(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String messageId = message.getMessageProperties().getMessageId(); // 需要生产者设置消息ID
        // 如果生产者未设置，可以使用 messageId 或自定义唯一标识，这里假设消息中有 fullShortUrl 等组合，但最好显式设置消息ID
        // 为简单起见，我们使用消息内容的哈希或自定义ID，但建议生产者在发送时设置 messageId
        // 此处采用 messageId 或生成一个唯一键
        if (messageId == null) {
            // 如果没有 messageId，可以自己构造一个，例如使用 deliveryTag 或其他
            // 这里演示使用 deliveryTag 作为幂等键的一部分，但注意多个节点可能重复？
            // 最好生产者设置唯一ID
            messageId = "msg_" + deliveryTag;
        }

        String body = new String(message.getBody());
        log.info("接收到消息，ID: {}, body: {}", messageId, body);

        // 1. 幂等检查
        if (!messageQueueIdempotentHandler.isMessageProcessed(messageId)) {
            // 消息已处理过，检查是否完成
            if (messageQueueIdempotentHandler.isAccomplish(messageId)) {
                // 已完成，直接确认消息
                try {
                    channel.basicAck(deliveryTag, false);
                } catch (IOException e) {
                    log.error("确认消息失败", e);
                }
                return;
            } else {
                // 未完成，需要重试（可能是之前处理失败）
                // 这里抛出异常让 Spring 重试，但需要确保幂等标识未删除？或者我们可以选择重新处理
                // 为了简化，我们删除幂等标识并重新处理
                messageQueueIdempotentHandler.delMessageProcessed(messageId);
                // 继续执行，重新处理
            }
        }

        try {
            // 2. 解析消息
            Map<String, String> producerMap = JSON.parseObject(body, Map.class);


            ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(producerMap.get("statsRecord"), ShortLinkStatsRecordDTO.class);
            // 3. 执行业务逻辑（复用原有 actualSaveShortLinkStats 方法）
            actualSaveShortLinkStats(statsRecord);


            // 4. 设置幂等完成标识
            messageQueueIdempotentHandler.setAccomplish(messageId);

            // 5. 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("消息处理成功，已确认: {}", messageId);
        } catch (Exception e) {
            log.error("消息处理异常，ID: {}", messageId, e);
            // 处理失败，删除幂等标识以便重试
            messageQueueIdempotentHandler.delMessageProcessed(messageId);
            try {
                // 拒绝消息，并重新入队（requeue = true）
                // 如果希望失败后不重新入队（如业务异常），可以设置 requeue = false，并可能进入死信队列
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("消息拒绝失败", ex);
            }
        }
    }
    public void actualSaveShortLinkStats(ShortLinkStatsRecordDTO statsRecord) {
        String fullShortUrl = statsRecord.getFullShortUrl();
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        try {
            //通过cookie判断当前用户是否为老用户

            //怕消费的时候使用的gid被修改了,所以这里重新查gid
            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, statsRecord.getFullShortUrl());
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
            String gid = shortLinkGotoDO.getGid();



            Date date = statsRecord.getCurrentDate();
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
                    .date(statsRecord.getCurrentDate())
                    .build();
            shortLinkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);

        }catch (Exception e){
            throw new ClientException("短链接访问统计异常");
        }finally {
            rLock.unlock();
        }
    }

}
