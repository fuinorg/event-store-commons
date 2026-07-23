# Resilience — what is implemented

How these libraries behave when the event store, the database or the key service cannot be reached, and what
you can configure.

These modules are the neutral foundation: plain Java, no Quarkus and no Spring. They do not decide *policy*
(how often to retry, when to trip a circuit breaker) — that belongs to the application and to the framework
integration layers. What they do provide is everything such a policy needs: **typed transient failures**,
**bounded calls**, and **classification you can act on**.

## Classifying a failure

Every "could not reach it" condition is reported as `org.fuin.esc.api.EscConnectionException`, so a retry or
circuit-breaker predicate is a single `instanceof`:

```java
catch(final RuntimeException ex){
        if(ex instanceof EscConnectionException){
        // transient - may be retried, subject to idempotency
        }
        }
```

This is deliberately distinct from the business exceptions of the API — `WrongExpectedVersionException`,
`StreamNotFoundException`, `StreamDeletedException`, `StreamAlreadyExistsException`,
`StreamReadOnlyException`, `ProjectionAlreadyExistsException`, `EventNotFoundException`. Those are *answers*
from the store and must never be retried: retrying cannot change them.

**Transient does not mean safe to repeat.** When a call fails this way the outcome is unknown — the failure
can happen after the store accepted the write and only the acknowledgement was lost. Reads can always be
repeated; appends need an expected version or a deduplication mechanism (see below).

## Bounded calls

Nothing waits forever. A call whose future is never completed — the connection was never established, or the
work item is queued behind a broken one — used to block the calling thread indefinitely.

| Where                          | Bound          | Configure with                                                       |
|--------------------------------|----------------|----------------------------------------------------------------------|
| gRPC event store, synchronous  | 5 s per call   | `ESGrpcEventStore.Builder.callTimeout(Duration)`                     |
| gRPC event store, asynchronous | 5 s per call   | `ESGrpcEventStoreAsync.Builder.callTimeout(Duration)`                |
| gRPC projection admin          | 5 s per call   | `new GrpcProjectionAdminEventStore(client, tenantContext, Duration)` |
| JPA queries                    | 5 s            | `JpaTimeouts` via `JpaEventStore.builder()`                          |
| JPA pessimistic locks          | 5 s            | `JpaTimeouts` via `JpaEventStore.builder()`                          |
| Key service (crypto)           | off by default | `EncryptingEventStore.Builder.keyServiceTimeout(Duration)`           |

A timeout is reported as `EventStoreCallTimeoutException`, which extends `EscConnectionException`, and is
deliberately *not* an `ExecutionException`: the callers interpret that as an answer from the server, so a
timeout reported that way would turn "no answer at all" into a wrong result.

The connection pool's own `connection-timeout` is a separate concern the application owns. Obtaining a
connection happens before any query runs, so the worst case is pool wait plus query timeout.

## `streamExists` never guesses

A connectivity failure makes `streamExists(..)` **throw**. It returns `false` only when the store actually
said the stream is not there. Answering `false` because the store could not be asked turns "I do not know"
into a business answer, and a caller acting on it would recreate a stream that already exists. A
hard-deleted stream still answers `false` — that is a real answer.

The same applies to the relational backend: `streamExists` on a projection stream throws when the database
is unreachable rather than reporting that the projection does not exist.

## Retrying an append

An append that failed transiently may or may not have been applied. Repeating it with `ExpectedVersion.ANY`
appends the events **a second time**. Pass the concrete version the stream is expected to have instead, and
the repetition of an append that did get through is not applied again.

How that is reported differs between backends, and a retrying caller must cope with both:

- **KurrentDB** answers with `WrongExpectedVersionException` — the version no longer matches.
- **The in-memory store** recognises that the stream already ends with exactly those events and answers with
  the current version, as if the call had just succeeded.

Either way the stream stays free of duplicates, which is the invariant to rely on — not the exception type.
A caller that cannot know the expected version needs its own deduplication; there is no other way to make a
retry safe.

## Reconnecting a dropped subscription

The stores deliver a dropped subscription to `onDrop` and then stay silent. `ReconnectingSubscribableEventStore`
(in `esc-spi`) decorates **any** `SubscribableEventStoreAsync` and re-establishes it:

```java
new ReconnectingSubscribableEventStore(store, scheduler, Backoff.DEFAULT);
```

`Backoff` (in `esc-api`) is exponential, capped and jittered — 500 ms doubling to a 30 s cap with 50% jitter
by default, and no attempt limit. The cap keeps a long outage from turning into an even longer recovery; the
jitter keeps every instance of a scaled-out service from reconnecting in lockstep and hitting the store as
one burst the moment it returns. The attempt counter resets whenever a subscription is established.

The handle you were given stays valid across reconnects, and unsubscribing from it stops both the current
subscription and any pending reconnect. Delivery is at-least-once: the resume position advances only after
your `onEvent` returns, so an event whose handler threw is redelivered.

Two limits worth knowing. A subscription started at an absolute event number resumes exactly; one started
with `SUBSCRIBE_TO_NEW_EVENTS` has no anchor and is re-established as "new events" again, so events written
during the outage are not redelivered — correct for a wake-up signal, but not gap-free delivery. And the
*first* subscribe is not retried: a failure there fails the returned future so you can see why your wiring
did not come up.

## Repeating projection administration

Projection admin calls are retried within a 30 s budget, with the delay coming from `Backoff`. The rule
differs per operation, because repeating some of them is observable:

- `projectionExists`, `enableProjection`, `disableProjection` are idempotent, so they retry on any transient
  status. Disabling additionally tolerates "not ready yet", which is what a freshly created projection
  answers while it initialises.
- `createProjection` and `deleteProjection` are **not** safely repeatable — a second create answers
  "Conflict" and a second delete answers "not found", so a repetition of a request that did arrive would be
  reported as a wrong business outcome. They retry only when the server certainly never took the request
  (`UNAVAILABLE`, `RESOURCE_EXHAUSTED`).

## PostgreSQL LISTEN/NOTIFY wake-ups

`PgListenNotifyWakeupSource` cuts projection latency by waking on `NOTIFY`, with a poll as the safety net.
Correctness never depends on the notification — the checkpoint does — so losing the listening connection may
only cost latency:

- while the channel is down the callback keeps firing on the poll interval, so the consumer degrades to
  exactly the plain poll it falls back to by design;
- given a `PgListenConnectionFactory`, the connection is re-opened and `LISTEN` re-issued with `Backoff`.
  Every successful reconnect fires the callback immediately, because notifications sent while nothing was
  listening are gone for good;
- when the backoff's attempts are used up it continues as a plain poll rather than giving up entirely.

A `PgListenConnectionFactory` is deliberately not a `DataSource`: a listening connection is held for the
lifetime of the source and would permanently remove one connection from your pool.

## Encryption and the key service

`EncryptingEventStore` calls an external key service on every encrypt and decrypt. A vault outage is reported
as `EscEncryptionConnectionException`, which extends `EscConnectionException` — so the same single
`instanceof` classifies it, and a consumer that wants to tell "the vault is down" from "the store is down"
can still do so.

**An outage never degrades into an undecryptable event.** `failOnUndecryptable(false)` returns the ciphertext
wrapper for an event whose key or key version is *permanently* gone. A vault that did not answer says nothing
about the event, so that failure always propagates instead — handing the wrapper back would let a caller take
ciphertext for data.

`keyServiceTimeout(Duration)` bounds how long a single key service call may block the appending or reading
thread. It is off by default, and it cannot abort the request: the call runs on an executor and keeps running
after the timeout, because nothing can interrupt a blocking socket read from outside. What it bounds is the
wait of the thread that must not hang. Your key service client's own connect/read timeout remains the better
primary bound.

# What is deliberately absent

**No resilience framework.** These modules ship no retry, circuit breaker, bulkhead or rate limiter, and
carry no dependency on one. Adding the framework-agnostic Resilience4j *core* modules and decorating the
store calls behind a builder flag was considered and rejected: the selling point of these libraries is that
they are plain Java, and everything a policy needs — typed transient exceptions, bounded calls, a reusable
`Backoff` — is here without one. Retry and circuit-breaker policy belongs to the framework integration layers
and to the applications, which know what an acceptable wait and an acceptable failure rate are.

**No configuration-property surface.** Timeouts and schedules are set through builders and constructors, not
read from `System.getProperty` or a configuration framework, so these modules stay free of any framework
dependency. An application maps its own configuration onto the builders.

## How this is verified

Beyond unit tests, the guarantees above are checked against real infrastructure with the failure injected:

- **KurrentDB** behind an in-process TCP proxy that can swallow or cut traffic: a blackholed call fails with
  a timeout after *at least* the configured bound (proving it waited and was cut short, rather than failing
  early for another reason), a cut connection produces `EscConnectionException` rather than a bare
  `RuntimeException`, `streamExists` throws instead of answering `false`, and the store recovers when
  traffic resumes.
- **PostgreSQL** via Testcontainers: a contended pessimistic lock gives up after the configured lock timeout
  — which also confirms Hibernate and the PostgreSQL dialect genuinely enforce it, rather than accepting the
  hint and ignoring it — and a database stopped under an open connection produces `EscConnectionException`.
