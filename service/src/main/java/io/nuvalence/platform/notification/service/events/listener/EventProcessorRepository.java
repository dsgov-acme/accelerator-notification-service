package io.nuvalence.platform.notification.service.events.listener;

import io.nuvalence.events.event.Event;
import io.nuvalence.events.event.NotificationEvent;
import io.nuvalence.events.exception.EventProcessingException;
import io.nuvalence.events.subscriber.EventProcessor;
import io.nuvalence.platform.notification.service.processors.NotificationEventProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for selecting the processor that corresponds to a given event.
 */
@Component
public class EventProcessorRepository {

    private final Map<String, Class<?>> eventProcessorsMap;
    private final ApplicationContext applicationContext;

    /**
     * Creates a new instance of the EventProcessorGetter and initialize the mapping between
     * event types and processors.
     * @param applicationContext used to get processor beans from spring context
     */
    public EventProcessorRepository(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.eventProcessorsMap = new HashMap<>();
        this.eventProcessorsMap.put(
                NotificationEvent.class.getSimpleName(), NotificationEventProcessor.class);
    }

    /**
     * Returns the processor that corresponds to the event type.
     * @param event Event from which the type is obtained
     * @return EventProcessor
     * @throws EventProcessingException if an error occurs during event processing.
     */
    public EventProcessor get(Event event) throws EventProcessingException {
        try {
            Class<?> eventProcessorClass = eventProcessorsMap.get(event.getMetadata().getType());
            EventProcessor eventProcessor =
                    (EventProcessor) applicationContext.getBean(eventProcessorClass);
            eventProcessor.setData(event);
            return eventProcessor;
        } catch (Exception e) {
            throw new EventProcessingException(e);
        }
    }
}
