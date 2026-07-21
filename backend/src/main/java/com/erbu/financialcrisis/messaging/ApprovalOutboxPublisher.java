package com.erbu.financialcrisis.messaging;

import com.erbu.financialcrisis.config.ApprovalRabbitConfig;
import com.erbu.financialcrisis.domain.entity.ApprovalOutbox;
import com.erbu.financialcrisis.mapper.ApprovalOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/** 只有收到 RabbitMQ Publisher Confirm 后才将 Outbox 标记为已发送。 */
@Component
@ConditionalOnProperty(name = "approval.messaging.enabled", havingValue = "true")
public class ApprovalOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(ApprovalOutboxPublisher.class);
    private final ApprovalOutboxMapper mapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public ApprovalOutboxPublisher(ApprovalOutboxMapper mapper, RabbitTemplate rabbitTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("${approval.messaging.publisher-batch-size:20}") int batchSize) {
        this.mapper = mapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${approval.messaging.publish-interval-ms:1000}")//定时扫描
    public void publishPending() {
        mapper.resetStalePublishing(LocalDateTime.now().minusMinutes(1));  //pending的时候服务宕机的话，超过一分钟会把卡住的记录回复成RETRY
         //查询待发送事件
        for (ApprovalOutbox event : mapper.selectPending(batchSize)) {
            //抢占发布权，数据库CAS抢占机制（001标记）
            if (mapper.claim(event.getId()) != 1) continue;
            try {
                //发布前转换成java对象
                ApprovalStartMessage payload = objectMapper.readValue(
                        event.getPayloadJson(), ApprovalStartMessage.class);

                CorrelationData correlation = new CorrelationData(event.getEventId());
                //发送RabbitMQ
                rabbitTemplate.convertAndSend(ApprovalRabbitConfig.APPROVAL_EXCHANGE,
                        event.getRoutingKey(), payload, message -> {
                            message.getMessageProperties().setMessageId(event.getEventId());
                            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            return message;
                        }, correlation);
                //最多等待10秒Broker
                CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
                if (!confirm.isAck()) throw new IllegalStateException("RabbitMQ NACK: " + confirm.getReason());
                //返回ack
                mapper.markPublished(event.getId());
            } catch (Exception ex) {
                //返回NACK，超时或连接失败
                log.warn("审批 Outbox 发送失败，eventId={}", event.getEventId(), ex);
                mapper.markRetry(event.getId(), safeMessage(ex), LocalDateTime.now().plusSeconds(5));
            }
        }
    }

    private String safeMessage(Exception ex) {
        String value = ex.getMessage();
        if (value == null) return ex.getClass().getSimpleName();
        return value.length() <= 900 ? value : value.substring(0, 900);
    }
}
