package org.zcdada.shortlink_zc.project.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 交换机名称
    public static final String STATS_EXCHANGE = "shortlink.stats.exchange";
    // 队列名称
    public static final String STATS_QUEUE = "shortlink.stats.queue";
    // 路由键
    public static final String STATS_ROUTING_KEY = "shortlink.stats.save";

    /**
     * 声明直连交换机
     */
    @Bean
    public DirectExchange statsExchange() {
        return new DirectExchange(STATS_EXCHANGE, true, false);
    }

    /**
     * 声明队列（持久化）
     */
    @Bean
    public Queue statsQueue() {
        return new Queue(STATS_QUEUE, true);
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding statsBinding() {
        return BindingBuilder.bind(statsQueue())
                .to(statsExchange())
                .with(STATS_ROUTING_KEY);
    }
}