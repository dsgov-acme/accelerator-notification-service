package io.nuvalence.platform.notification.service.config;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.google.pubsub.v1.Subscription;
import io.nuvalence.platform.notification.service.service.NotificationProcessingSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Configures PubSub Inbound.
 */
@Slf4j
@Configuration
public class PubSubInboundConfig {

    private static final String INPUT_CHANNEL = "inputMessageChannel";

    private final String topic;

    private final String deadLetterTopic;

    private final String subscription;

    private final boolean createTopicAndSubs;

    private final NotificationProcessingSubscriber subscriber;

    /**
     * PubSub config constructor.
     *
     * @param topic              the name of the topic to publish messages to
     * @param deadLetterTopic    the name of the dead letter topic for failed requests
     * @param subscription       the name of the subscription to pull messages from
     * @param createTopicAndSubs whether to create the topic and subscription if they don't exist
     * @param subscriber         the subscriber bean
     */
    public PubSubInboundConfig(
            @Value("${spring.cloud.gcp.pubsub.topic}") String topic,
            @Value("${spring.cloud.gcp.pubsub.deadLetterTopic}") String deadLetterTopic,
            @Value("${spring.cloud.gcp.pubsub.subscription2}") String subscription,
            @Value("${spring.cloud.gcp.pubsub.enableTopicCreation}") boolean createTopicAndSubs,
            NotificationProcessingSubscriber subscriber) {
        this.subscription = subscription;
        this.topic = topic;
        this.createTopicAndSubs = createTopicAndSubs;
        this.subscriber = subscriber;
        this.deadLetterTopic = deadLetterTopic;
    }

    /**
     * Creates a message channel that receives messages from PubSub.
     *
     * @return Message Channel
     */
    @Bean
    public MessageChannel inputMessageChannel() {
        return new DirectChannel();
    }

    /**
     * Creates a message adapter that receives messages from PubSub.
     *
     * @param inputChannel Message Channel
     * @param pubSubTemplate PubSub Message Template
     * @param admin PubSub Admin
     * @return Message Adapter
     */
    @Bean
    public PubSubInboundChannelAdapter inboundChannelAdapter(
            @Qualifier(INPUT_CHANNEL) MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate,
            PubSubAdmin admin) {
        if (createTopicAndSubs && admin.getTopic(topic) == null) {
            log.info("Creating topic: {}", topic);
            admin.createTopic(topic);
        }
        if (createTopicAndSubs && admin.getTopic(deadLetterTopic) == null) {
            log.info("Creating topic: {}", deadLetterTopic);
            admin.createTopic(deadLetterTopic);
        }
        if (createTopicAndSubs && admin.getSubscription(subscription) == null) {
            log.info("Creating subscription: {}, topic: {}", subscription, topic);

            DeadLetterPolicy deadLetterPolicy =
                    DeadLetterPolicy.newBuilder()
                            .setDeadLetterTopic(deadLetterTopic)
                            .setMaxDeliveryAttempts(5)
                            .build();

            Subscription.Builder subscriptionBuilder =
                    Subscription.newBuilder()
                            .setName(subscription)
                            .setTopic(topic)
                            .setAckDeadlineSeconds(20)
                            .setDeadLetterPolicy(deadLetterPolicy);

            admin.createSubscription(subscriptionBuilder);
        }

        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, subscription);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    /**
     * Creates a message handler that receives messages from PubSub.
     *
     * @return Message Handler
     */
    @ServiceActivator(inputChannel = INPUT_CHANNEL)
    @Bean
    public MessageHandler messageReceiverNotificationProcessing() {
        return subscriber;
    }
}
