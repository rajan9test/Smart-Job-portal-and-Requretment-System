package com.jobportal.event;

/** Observer in the Observer pattern. */
@FunctionalInterface
public interface PortalEventListener {
    void onEvent(PortalEvent event);
}
