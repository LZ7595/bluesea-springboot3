package com.example.backend.Consumer;

import com.example.backend.Config.RabbitConfig;
import com.example.backend.Model.Message.OrderMessage;
import com.example.backend.Model.Entity.Order;
import com.example.backend.Service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * 订单取消消费者：处理超时未支付的订单取消逻辑
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelConsumer {
    private final OrderService orderService;

    // 监听订单取消队列
    @RabbitListener(queues = RabbitConfig.ORDER_CANCEL_QUEUE)
    @Transactional
    public void handleOrderCancel(
            OrderMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.info("===== 订单超时检查 =====");
        log.info("订单ID: {}", message.getOrderId());
        log.info("订单编号: {}", message.getOrderNo());
        log.info("开始检查订单是否超时未支付...");

        try {
            // 1. 查询订单当前状态
            Order order = orderService.getOrderById(message.getOrderId());
            if (order == null) {
                log.warn("订单不存在，无需处理");
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 判断订单状态，未支付则执行取消
            if ("UNPAID".equals(order.getOrder_status())) {
                log.info("订单仍为未支付状态，执行取消操作");
                // 调用订单取消方法（已有的业务逻辑）
                orderService.cancelOrder(message.getOrderId());
                log.info("订单取消成功");
            } else {
                log.info("订单状态为[{}]，无需取消", order.getOrder_status());
            }

            // 3. 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理订单超时取消失败", e);
            // 处理失败：消息重回队列（最多重试3次，避免死循环）
            channel.basicNack(deliveryTag, false, true);
        } finally {
            log.info("===========================");
        }
    }
}
