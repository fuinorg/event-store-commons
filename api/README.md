# esc-api
Defines the event store commons API.

| Interface (Synchronous) | Description |
|+------------------------|+------------|
| [ReadableEventStoreSync](src/main/java/org/fuin/esc/api/ReadableEventStoreSync.java) | Read only functionality |
| [WritableEventStoreSync](src/main/java/org/fuin/esc/api/WritableEventStoreSync.java) | Write Only functionality |
| [EventStoreSync](src/main/java/org/fuin/esc/api/EventStoreSync.java) | Combined read and write functionality |
| [SubscribableEventStoreSync](src/main/java/org/fuin/esc/api/SubscribableEventStoreSync.java) | Handles volatile subscriptions |

| Interface (Asynchronous) | Description |
|+-------------------------|+------------|
| [ReadableEventStoreAsync](src/main/java/org/fuin/esc/api/ReadableEventStoreAsync.java) | Read only functionality |
| [WritableEventStoreAsync](src/main/java/org/fuin/esc/api/WritableEventStoreAsync.java) | Write Only functionality |
| [EventStoreAsync](src/main/java/org/fuin/esc/api/EventStoreAsync.java) | Combined read and write functionality |
| [SubscribableEventStoreAsync](src/main/java/org/fuin/esc/api/SubscribableEventStoreAsync.java) | Handles volatile subscriptions |

Simple delegating asynchronous event store that uses a synchronous one internally: [DelegatingAsyncEventStore](src/main/java/org/fuin/esc/api/DelegatingAsyncEventStore.java)