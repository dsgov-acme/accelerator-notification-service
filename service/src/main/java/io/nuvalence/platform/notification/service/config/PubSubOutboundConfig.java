package io.nuvalence.platform.notification.service.config;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Configures PubSub Outbound.
 */
@Slf4j
@Configuration
public class PubSubOutboundConfig {

    private static final String OUTPUT_CHANNEL = "pubSubOutputChannel";
    private final String topic;
    private final boolean createTopicAndSubs;

    /**
     * PubSub config constructor.
     *
     * @param topic              the name of the topic to publish messages to
     * @param createTopicAndSubs whether to create the topic and subscription if they don't exist
     */
    public PubSubOutboundConfig(
            @Value("${spring.cloud.gcp.pubsub.topic}") String topic,
            @Value("${spring.cloud.gcp.pubsub.enableTopicCreation}") boolean createTopicAndSubs) {
        this.topic = topic;
        this.createTopicAndSubs = createTopicAndSubs;
    }

    /**
     * Creates a message handler that sends messages to PubSub.
     *
     * @param pubsubTemplate PubSub Message Template
     * @param admin PubSub Admin
     * @return Message Handler
     */
    @Bean
    @ServiceActivator(inputChannel = OUTPUT_CHANNEL)
    public MessageHandler messageSender(PubSubTemplate pubsubTemplate, PubSubAdmin admin) {
        if (createTopicAndSubs && admin.getTopic(topic) == null) {
            log.info("Creating topic: {}", topic);
            admin.createTopic(topic);
        }
        return new PubSubMessageHandler(pubsubTemplate, topic);
    }

    /**
     * Creates a message handler for unit testing purposes.
     *
     * @return Message Handler
     */
    @Bean
    @ConditionalOnProperty(value = "spring.cloud.gcp.pubsub.unit-test", havingValue = "true")
    @ServiceActivator(inputChannel = OUTPUT_CHANNEL)
    public MessageHandler localMessageSender() {
        return message -> {
            String payload = (String) message.getPayload();
            log.info("Message sent to local channel: {}", payload);
        };
    }

    /**
     * Creates a message channel for PubSub.
     *
     */
    @MessagingGateway(defaultRequestChannel = OUTPUT_CHANNEL)
    public interface PubSubOutboundGateway {
        void publish(Message<String> message);
    }
}
