package com.example.backend.Consumer;

import com.example.backend.Config.RabbitConfig;
import com.example.backend.Model.Message.OrderMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单通知消费者：处理订单创建后的后续操作（如日志记录）
 */
@Component
@Slf4j
public class OrderNotifyConsumer {

    // 监听订单通知队列
    @RabbitListener(queues = RabbitConfig.ORDER_NOTIFY_QUEUE)
    public void handleOrderNotify(
            OrderMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.info("===== 订单通知消息处理 =====");
        log.info("订单ID: {}", message.getOrderId());
        log.info("订单编号: {}", message.getOrderNo());
        log.info("用户ID: {}", message.getUserId());
        log.info("用户手机号: {}", message.getPhone());
        log.info("订单状态: {}", message.getOrderStatus());
        log.info("订单创建成功，已记录系统日志");
        log.info("===========================");

        // 手动确认消息：处理成功，从队列移除
        channel.basicAck(deliveryTag, false);
    }
}
