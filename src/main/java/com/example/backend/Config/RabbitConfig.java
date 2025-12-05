package com.example.backend.Config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    // ========================== 订单通知相关配置（原有） ==========================
    public static final String ORDER_NOTIFY_EXCHANGE = "order.notify.exchange";
    public static final String ORDER_NOTIFY_QUEUE = "order.notify.queue";
    public static final String ORDER_NOTIFY_ROUTING_KEY = "order.created";

    // ========================== 订单超时取消相关配置（原有） ==========================
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.delay.cancel";

    // ========================== 商品缓存更新相关配置（新增） ==========================
    // 1. 商品缓存更新交换机
    public static final String PRODUCT_CACHE_EXCHANGE = "product.cache.exchange";
    // 2. 商品缓存更新队列（处理实时缓存删除）
    public static final String PRODUCT_CACHE_UPDATE_QUEUE = "product.cache.update.queue";
    // 3. 商品缓存更新路由键
    public static final String PRODUCT_CACHE_UPDATE_ROUTING_KEY = "cache.update";

    // 4. 促销延迟交换机（处理促销结束时的缓存更新）
    public static final String PROMOTION_DELAY_EXCHANGE = "promotion.delay.exchange";
    // 5. 促销延迟队列（暂存消息，过期后触发缓存更新）
    public static final String PROMOTION_DELAY_QUEUE = "promotion.delay.queue";
    // 6. 促销延迟路由键
    public static final String PROMOTION_DELAY_ROUTING_KEY = "delay.end";


    /**
     * 1. 定义JSON消息转换器（替代默认的Java原生序列化）
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 2. 配置消息监听容器工厂，指定使用JSON转换器
     * （所有@RabbitListener注解的方法会用这个工厂，确保消息转换方式一致）
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,  // Spring会自动注入RabbitMQ连接工厂
            MessageConverter jsonMessageConverter) {  // 注入上面定义的JSON转换器

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);  // 关联RabbitMQ连接
        factory.setMessageConverter(jsonMessageConverter);  // 关键：使用JSON转换器
        return factory;
    }

    /**
     * 3. 配置RabbitTemplate（发送消息时使用），确保发送和接收用相同的转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);  // 发送消息也用JSON格式
        return rabbitTemplate;
    }

    // ========================== 原有订单相关Bean（保持不变） ==========================
    @Bean
    public DirectExchange orderNotifyExchange() {
        return ExchangeBuilder.directExchange(ORDER_NOTIFY_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderNotifyQueue() {
        return QueueBuilder.durable(ORDER_NOTIFY_QUEUE)
                .build();
    }

    @Bean
    public Binding orderNotifyBinding() {
        return BindingBuilder.bind(orderNotifyQueue())
                .to(orderNotifyExchange())
                .with(ORDER_NOTIFY_ROUTING_KEY);
    }

    @Bean
    public CustomExchange orderDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(ORDER_DELAY_EXCHANGE, "x-delayed-message", true, false, args);
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_QUEUE)
                .build();
    }

    @Bean
    public Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue())
                .to(orderDelayExchange())
                .with(ORDER_CANCEL_ROUTING_KEY)
                .noargs();
    }


    // ========================== 新增商品缓存相关Bean ==========================
    // 商品缓存更新交换机
    @Bean
    public DirectExchange productCacheExchange() {
        return ExchangeBuilder.directExchange(PRODUCT_CACHE_EXCHANGE)
                .durable(true)  // 持久化
                .build();
    }

    // 商品缓存更新队列
    @Bean
    public Queue productCacheUpdateQueue() {
        return QueueBuilder.durable(PRODUCT_CACHE_UPDATE_QUEUE)
                .build();
    }

    // 绑定缓存更新队列到交换机
    @Bean
    public Binding productCacheUpdateBinding() {
        return BindingBuilder.bind(productCacheUpdateQueue())
                .to(productCacheExchange())
                .with(PRODUCT_CACHE_UPDATE_ROUTING_KEY);
    }

    // 促销延迟交换机（使用延迟消息类型）
    @Bean
    public CustomExchange promotionDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");  // 支持延迟转发
        return new CustomExchange(PROMOTION_DELAY_EXCHANGE, "x-delayed-message", true, false, args);
    }

    // 促销延迟队列（消息过期后转发到缓存更新队列）
    @Bean
    public Queue promotionDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 死信交换机：消息过期后转发到商品缓存交换机
        args.put("x-dead-letter-exchange", PRODUCT_CACHE_EXCHANGE);
        // 死信路由键：转发到缓存更新的路由键
        args.put("x-dead-letter-routing-key", PRODUCT_CACHE_UPDATE_ROUTING_KEY);
        return QueueBuilder.durable(PROMOTION_DELAY_QUEUE)
                .withArguments(args)
                .build();
    }

    // 绑定促销延迟队列到延迟交换机
    @Bean
    public Binding promotionDelayBinding() {
        return BindingBuilder.bind(promotionDelayQueue())
                .to(promotionDelayExchange())
                .with(PROMOTION_DELAY_ROUTING_KEY)
                .noargs();
    }
}