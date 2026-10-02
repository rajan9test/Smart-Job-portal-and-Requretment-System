package com.jobportal.notification;

import com.jobportal.TestData;
import com.jobportal.event.EventPublisher;
import com.jobportal.event.PortalEvent;
import com.jobportal.model.Notification;
import com.jobportal.model.NotificationChannel;
import com.jobportal.repository.inmemory.InMemoryNotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationDispatcherTest {

    private final InMemoryNotificationRepository repository = new InMemoryNotificationRepository();

    @Test
    void deliversEverythingSubmittedFromManyThreads() throws InterruptedException {
        NotificationDispatcher dispatcher = new NotificationDispatcher(new NotificationFactory(repository), 4);
        dispatcher.start();

        int producers = 8;
        int perProducer = 250;
        ExecutorService pool = Executors.newFixedThreadPool(producers);
        CountDownLatch done = new CountDownLatch(producers);
        for (int p = 0; p < producers; p++) {
            long userId = p;
            pool.submit(() -> {
                for (int i = 0; i < perProducer; i++) {
                    dispatcher.submit(new Notification(userId, "msg " + i, NotificationChannel.IN_APP, LocalDateTime.now()));
                }
                done.countDown();
            });
        }
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdown();

        dispatcher.close(); // drains the queue before returning

        assertEquals(producers * perProducer, dispatcher.deliveredCount());
        assertEquals(producers * perProducer, repository.count());
        assertEquals(0, dispatcher.pendingCount());
    }

    @Test
    void rejectsWorkWhenNotRunning() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(new NotificationFactory(repository), 1);
        assertThrows(IllegalStateException.class, () -> dispatcher.submit(
                new Notification(1L, "x", NotificationChannel.EMAIL, LocalDateTime.now())));
    }

    @Test
    void applicationSubmittedNotifiesRecruiterOnEveryConfiguredChannel() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(new NotificationFactory(repository), 1);
        dispatcher.start();
        EventPublisher events = new EventPublisher();
        events.subscribe(new NotificationEventListener(dispatcher,
                List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL), TestData.CLOCK));

        events.publish(new PortalEvent.ApplicationSubmitted(1L, 10L, "Rajan", 50L, "Java Developer"));
        dispatcher.close();

        assertEquals(2, dispatcher.deliveredCount());
        List<Notification> inbox = repository.findByRecipientUserId(50L); // only IN_APP is stored
        assertEquals(1, inbox.size());
        assertEquals("Rajan applied for Java Developer", inbox.getFirst().getMessage());
    }
}
