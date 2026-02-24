package org.zcdada.shortlink_zc.project.mq.producer;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.zcdada.shortlink_zc.project.mq.config.RabbitMQConfig.STATS_EXCHANGE;
import static org.zcdada.shortlink_zc.project.mq.config.RabbitMQConfig.STATS_ROUTING_KEY;

@AllArgsConstructor
@Component
public class ShortLinkStatsSaveProducerRabbitMq {
    private final RabbitTemplate rabbitTemplate;
    public void send(Map<String, String> producerMap) {
        String message = JSON.toJSONString(producerMap);
        Message messageObj = MessageBuilder.withBody(message.getBytes(StandardCharsets.UTF_8))
                .setMessageId(UUID.randomUUID().toString())
                .build();
        rabbitTemplate.send(STATS_EXCHANGE, STATS_ROUTING_KEY, messageObj);
    }
}
