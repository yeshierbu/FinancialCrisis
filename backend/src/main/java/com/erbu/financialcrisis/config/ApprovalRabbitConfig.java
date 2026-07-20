package com.erbu.financialcrisis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RabbitMQ 审批消息拓扑配置。
 *
 * <p>这套配置把异步审批拆成三类队列：主队列负责正常执行审批步骤，重试队列负责失败后的
 * 延迟重投，死信队列负责保存最终失败的消息，便于后续人工排查和补偿处理。</p>
 */
@Configuration
@EnableRabbit
@EnableScheduling
@ConditionalOnProperty(name = "approval.messaging.enabled", havingValue = "true")
public class ApprovalRabbitConfig {
    /** 正常审批任务使用的主交换机。 */
    public static final String APPROVAL_EXCHANGE = "approval.exchange";

    /** 正常审批任务消费队列，消费者从这里拉取并执行审批步骤。 */
    public static final String APPROVAL_QUEUE = "approval.start.queue";

    /** 主审批消息路由键，用于把消息投递到主队列。 */
    public static final String APPROVAL_ROUTING_KEY = "approval.start";

    /** 失败重试消息使用的交换机。 */
    public static final String RETRY_EXCHANGE = "approval.retry.exchange";

    /** 延迟重试队列，消息会在这里等待 TTL 到期。 */
    public static final String RETRY_QUEUE = "approval.retry.queue";

    /** 重试消息路由键，用于把失败消息投递到重试队列。 */
    public static final String RETRY_ROUTING_KEY = "approval.retry";

    /** 死信交换机，用于接收最终无法继续处理的审批消息。 */
    public static final String DEAD_EXCHANGE = "approval.dlx";

    /** 死信队列，保存超过最大重试次数或被显式转死信的消息。 */
    public static final String DEAD_QUEUE = "approval.dead.queue";

    /** 死信消息路由键，用于把消息投递到死信队列。 */
    public static final String DEAD_ROUTING_KEY = "approval.dead";

    /** 创建持久化主交换机，服务重启后交换机仍然存在。 */
    @Bean
    DirectExchange approvalExchange() {
        return new DirectExchange(APPROVAL_EXCHANGE, true, false);
    }

    /**
     * 创建主审批队列。
     *
     * <p>如果主队列中的消息被 RabbitMQ 判定为死信，会按配置转发到死信交换机和死信路由键。</p>
     */
    @Bean
    Queue approvalQueue() {
        return QueueBuilder.durable(APPROVAL_QUEUE)
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    /** 将主审批队列绑定到主交换机，只有匹配 approval.start 的消息才会进入主队列。 */
    @Bean
    Binding approvalBinding() {
        return BindingBuilder.bind(approvalQueue()).to(approvalExchange()).with(APPROVAL_ROUTING_KEY);
    }

    /** 创建持久化重试交换机，用于接收需要延迟重试的审批消息。 */
    @Bean
    DirectExchange retryExchange() {
        return new DirectExchange(RETRY_EXCHANGE, true, false);
    }

    /**
     * 创建延迟重试队列。
     *
     * <p>消息进入该队列后不会立即消费，而是等待 TTL 到期；到期后作为死信重新投递回
     * 主审批交换机，从而实现“延迟后再执行一次审批步骤”。</p>
     */
    @Bean
    Queue retryQueue(@Value("${approval.messaging.retry-delay-ms:30000}") int delayMs) {
        return QueueBuilder.durable(RETRY_QUEUE)
                .ttl(delayMs)
                .deadLetterExchange(APPROVAL_EXCHANGE)
                .deadLetterRoutingKey(APPROVAL_ROUTING_KEY)
                .build();
    }

    /** 将重试队列绑定到重试交换机，供消费者失败时投递重试消息。 */
    @Bean
    Binding retryBinding(@Qualifier("retryQueue") Queue queue,
                         @Qualifier("retryExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(RETRY_ROUTING_KEY);
    }

    /** 创建持久化死信交换机，用于集中接收不可恢复的失败消息。 */
    @Bean
    DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    /** 创建死信队列，便于管理员排查失败原因或做人工补偿。 */
    @Bean
    Queue deadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    /** 将死信队列绑定到死信交换机。 */
    @Bean
    Binding deadBinding() {
        return BindingBuilder.bind(deadQueue()).to(deadExchange()).with(DEAD_ROUTING_KEY);
    }

    /** 定义 RabbitMQ JSON 消息转换器，让审批消息可以在 Java 对象和 JSON 之间自动转换。 */
    @Bean
    MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        // 复用 Spring 的 ObjectMapper，确保 LocalDateTime 等项目类型序列化规则一致。
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
