# esc-api

Defines the event store commons API.

| Interface (Synchronous)                                                              | Description                           |
|:-------------------------------------------------------------------------------------|:--------------------------------------|
| [ReadableEventStore](src/main/java/org/fuin/esc/api/ReadableEventStore.java)         | Read only functionality               |
| [WritableEventStore](src/main/java/org/fuin/esc/api/WritableEventStore.java)         | Write Only functionality              |
| [EventStore](src/main/java/org/fuin/esc/api/EventStore.java)                         | Combined read and write functionality |
| [SubscribableEventStore](src/main/java/org/fuin/esc/api/SubscribableEventStore.java) | Handles volatile subscriptions        |

| Interface (Asynchronous)                                                                       | Description                           |
|:-----------------------------------------------------------------------------------------------|:--------------------------------------|
| [ReadableEventStoreAsync](src/main/java/org/fuin/esc/api/ReadableEventStoreAsync.java)         | Read only functionality               |
| [WritableEventStoreAsync](src/main/java/org/fuin/esc/api/WritableEventStoreAsync.java)         | Write Only functionality              |
| [EventStoreAsync](src/main/java/org/fuin/esc/api/EventStoreAsync.java)                         | Combined read and write functionality |
| [SubscribableEventStoreAsync](src/main/java/org/fuin/esc/api/SubscribableEventStoreAsync.java) | Handles volatile subscriptions        |

Simple delegating asynchronous event store that uses a synchronous one
internally: [DelegatingAsyncEventStore](src/main/java/org/fuin/esc/api/DelegatingAsyncEventStore.java)