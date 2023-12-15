package io.nuvalence.platform.notification.service.events.listener;

import io.nuvalence.events.event.Event;
import io.nuvalence.events.event.EventMetadata;
import io.nuvalence.events.event.NotificationEvent;
import io.nuvalence.events.exception.EventProcessingException;
import io.nuvalence.events.subscriber.EventProcessor;
import io.nuvalence.platform.notification.service.processors.NotificationEventProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class EventProcessorGetterTest {
    @InjectMocks private EventProcessorRepository eventProcessorGetter;

    @Mock private ApplicationContext applicationContext;

    @Test
    void testGetEventProcessor() throws EventProcessingException {
        NotificationEvent event = new NotificationEvent();
        event.setMetadata(new EventMetadata());
        event.getMetadata().setType(NotificationEvent.class.getSimpleName());

        Mockito.when(applicationContext.getBean(NotificationEventProcessor.class))
                .thenReturn(new NotificationEventProcessor(null, null, null, null, null));

        EventProcessor result = eventProcessorGetter.get(event);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(NotificationEventProcessor.class, result.getClass());
    }

    @Test()
    void testGetEventProcessorWithInvalidEventType() {
        Event event = new NotificationEvent();
        event.setMetadata(new EventMetadata());
        event.getMetadata().setType("invalidEventType");

        Assertions.assertThrows(
                EventProcessingException.class, () -> eventProcessorGetter.get(event));
    }
}
