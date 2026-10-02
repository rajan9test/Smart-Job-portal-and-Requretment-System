# 09 · Events & notifications

**Packages/classes:** `event.*`, `notification.*`, `service.InboxService`

When something important happens, the right person is told about it, without the service that caused it knowing who listens or how messages are delivered.

## Flow

```
ApplicationService / InterviewService
        │ events.publish(new PortalEvent.ApplicationSubmitted(...))
        ▼
EventPublisher  ── for each subscribed PortalEventListener ──►  NotificationEventListener
 (Observer subject)                                               decides WHO and WHAT
                                                                     │ dispatcher.submit(notification) × channels
                                                                     ▼
                                                    NotificationDispatcher
                                                    LinkedBlockingQueue ──► worker thread(s)
                                                                                │ factory.create(channel).send(n)
                                                                                ▼
                                                    EmailNotificationService   (logs, simulated)
                                                    SMSNotificationService     (logs, truncated to 160 chars)
                                                    InAppNotificationService   (saves to NotificationRepository)
```

## Events (`PortalEvent`, sealed interface)

| Event | Raised by | Who is notified |
|-------|-----------|-----------------|
| `ApplicationSubmitted` | `ApplicationService.apply` | recruiter who posted the job |
| `ApplicationStatusChanged` | status change / withdraw | candidate (or the recruiter, if the candidate withdrew) |
| `InterviewScheduled` | `InterviewService.schedule` | candidate |
| `InterviewRescheduled` | `reschedule` | candidate |
| `InterviewCancelled` | `cancel` | candidate |

Because `PortalEvent` is **sealed**, the `switch` in `NotificationEventListener` is checked for exhaustiveness: add a new event and forget to handle it, and the code won't compile.

`EventPublisher` stores listeners in a `CopyOnWriteArrayList` and catches exceptions per listener, so a broken listener can't undo the business operation.

## Delivery

| Class | Pattern / concept |
|-------|-------------------|
| `NotificationService` | interface: `channel()`, `send(Notification)` (dependency inversion) |
| `NotificationFactory` | **Factory**: `create(channel)` returns the matching service (cached in an `EnumMap`) |
| `NotificationDispatcher` | **Producer/consumer** with `BlockingQueue` + `ExecutorService` |

`NotificationDispatcher` details:

- `start()` launches `notification.worker.threads` daemon workers that `poll` the queue
- `submit(n)` is non-blocking for the caller; it throws `IllegalStateException` if the dispatcher isn't running
- a failed send is logged and counted (`failedCount()`); it never kills the worker
- `close()` stops accepting work, lets workers **drain** the queue, and waits up to 5 s
- counters `deliveredCount()`, `failedCount()`, `pendingCount()` are useful in tests

Channels per notification come from `notification.channels` (default `IN_APP,EMAIL`).

Delivery is asynchronous, so a new notification can take a few milliseconds to appear in the inbox.

## Inbox: `InboxService`

Read side of in-app notifications: `getInbox(user)` (newest first), `unreadCount(user)`, `markRead(user, id)` (own notifications only), `markAllRead(user)`.

| Method | Path |
|--------|------|
| GET | `/api/notifications` → `{unread, items:[{id, message, createdAt, read}]}` |
| POST | `/api/notifications/{id}/read` |
| POST | `/api/notifications/read-all` |

## Extending

- **Push notifications**: add `PUSH` to `NotificationChannel`, write `PushNotificationService`, add the case to `NotificationFactory`. Nothing else changes.
- **Audit log**: write another `PortalEventListener` and `subscribe` it in `PortalApplication`.
- **Phase 5 ideas**: retry with exponential backoff, `CompletableFuture<Void>` from `submit`, a dead-letter queue, queue-depth metrics.
- **Phase 6**: replace the in-process queue with Kafka/RabbitMQ.

## Tests

`NotificationDispatcherTest`: 8 producer threads × 250 notifications all delivered exactly once after `close()`; submitting when stopped fails; an `ApplicationSubmitted` event produces one notification per configured channel for the recruiter.
