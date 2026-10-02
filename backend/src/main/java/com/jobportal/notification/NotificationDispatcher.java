package com.jobportal.notification;

import com.jobportal.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Producer/consumer: services enqueue notifications and return immediately; a small pool of
 * worker threads drains the queue and delivers through the right channel.
 *
 * <pre>
 * ApplicationService --submit()--> [ BlockingQueue ] --take--> worker-1 --> Email/SMS/In-App
 *                                                    \-take--> worker-2 --> ...
 * </pre>
 *
 * Phase 5 exercises: add retry with backoff for failed sends, return a CompletableFuture from
 * submit(), and expose queue depth as a metric.
 */
public class NotificationDispatcher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final long POLL_MILLIS = 100;

    private final BlockingQueue<Notification> queue = new LinkedBlockingQueue<>();
    private final NotificationFactory factory;
    private final int workerCount;
    private final ExecutorService workers;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong delivered = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public NotificationDispatcher(NotificationFactory factory, int workerCount) {
        if (workerCount < 1) throw new IllegalArgumentException("workerCount must be >= 1");
        this.factory = factory;
        this.workerCount = workerCount;
        AtomicInteger threadNo = new AtomicInteger();
        this.workers = Executors.newFixedThreadPool(workerCount, runnable -> {
            Thread t = new Thread(runnable, "notify-worker-" + threadNo.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (!running.compareAndSet(false, true)) return;
        for (int i = 0; i < workerCount; i++) {
            workers.submit(this::workerLoop);
        }
        log.info("Notification dispatcher started with {} worker(s)", workerCount);
    }

    /** Non-blocking for the caller: the notification is delivered later on a worker thread. */
    public void submit(Notification notification) {
        if (!running.get()) {
            throw new IllegalStateException("Dispatcher is not running");
        }
        queue.offer(notification);
    }

    private void workerLoop() {
        // Keep draining after shutdown is requested so nothing already queued is lost.
        while (running.get() || !queue.isEmpty()) {
            try {
                Notification next = queue.poll(POLL_MILLIS, TimeUnit.MILLISECONDS);
                if (next != null) {
                    deliver(next);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void deliver(Notification notification) {
        try {
            factory.create(notification.getChannel()).send(notification);
            delivered.incrementAndGet();
        } catch (RuntimeException e) {
            failed.incrementAndGet();
            log.error("Failed to deliver {}", notification, e);
        }
    }

    public long deliveredCount() {
        return delivered.get();
    }

    public long failedCount() {
        return failed.get();
    }

    public int pendingCount() {
        return queue.size();
    }

    /** Stops accepting work, lets workers drain the queue, then waits for them to finish. */
    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) return;
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Workers did not finish in time; {} notification(s) dropped", queue.size());
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Notification dispatcher stopped (delivered={}, failed={})", delivered.get(), failed.get());
    }
}
