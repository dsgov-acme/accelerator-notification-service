package io.nuvalence.platform.notification.service.service;

import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.platform.notification.service.config.PubSubOutboundConfig;
import io.nuvalence.platform.notification.service.domain.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
class PubSubServiceTest {

    @Mock private PubSubOutboundConfig.PubSubOutboundGateway messagingGateway;
    @Mock private ObjectMapper mockMapper;

    private PubSubService service;

    @BeforeEach
    public void beforeEach() {
        service = new PubSubService(messagingGateway, mockMapper);
    }

    @Test
    void testPublish() throws IOException {
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setUserId(UUID.randomUUID().toString());
        message.setMessageTemplateKey("messageTemplateKey");
        message.setStatus("QUEUED");
        message.setParameters(Map.of("parameter-key", "parameter-value"));
        message.setRequestedTimestamp(OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS));

        Mockito.when(mockMapper.writeValueAsString(any(Message.class)))
                .thenReturn("serialized-string");

        service.publish(message);

        Mockito.verify(messagingGateway).publish(any());
    }
}
