package io.nuvalence.platform.notification.service.events.listener;

import io.nuvalence.events.event.Event;
import io.nuvalence.events.exception.EventProcessingException;
import io.nuvalence.events.subscriber.EventProcessor;
import io.nuvalence.events.subscriber.listener.EventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * This class is the entry point for events in the subscriber app.
 */
@Service
@RequiredArgsConstructor
public class EventListenerImpl implements EventListener {
    private final EventProcessorRepository eventProcessorGetter;

    @Override
    public void onEvent(Event event) throws EventProcessingException {
        EventProcessor someEventProcessor = eventProcessorGetter.get(event);
        someEventProcessor.execute();
    }
}
