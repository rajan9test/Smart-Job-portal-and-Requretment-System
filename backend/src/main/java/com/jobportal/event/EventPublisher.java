package com.jobportal.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject in the Observer pattern. Services publish events here without knowing who listens,
 * so adding (say) an audit-log listener never requires touching ApplicationService.
 */
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    /** CopyOnWriteArrayList: safe to iterate while another thread subscribes. Reads vastly outnumber writes. */
    private final List<PortalEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(PortalEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(PortalEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(PortalEvent event) {
        log.debug("Publishing {}", event);
        for (PortalEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                // A broken listener must not roll back the business operation that raised the event.
                log.error("Listener {} failed on {}", listener, event, e);
            }
        }
    }
}
