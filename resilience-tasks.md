# Resilience Tasks — event-store-commons (esc)

Part of the [Resilience Roadmap](https://github.com/fuinorg/ddd-cqrs-4-java-example/blob/develop/resilience-roadmap.md). This layer is the
**neutral foundation** (plain Java, no Quarkus/Spring): it must supply *typed transient exceptions*,
*timeouts at the boundary*, and *classification* so the framework layers (SmallRye FT on Quarkus,
Resilience4j on Spring) can apply Retry/CircuitBreaker/Bulkhead/Fallback intelligently.

**Do not add SmallRye FT / Resilience4j dependencies to these modules by default.** (Optional path: the
framework-agnostic Resilience4j *core* modules could decorate the store programmatically — see the last
section; keep it opt-in.)

Legend: `[ ]` todo · scenario tags **S1** (event store) / **S2** (database) / **S5** (crypto/vault, the
`EncryptingEventStore` half).

---

## Phase 0 — DONE (2026-07-22)

Implemented and released as `0.10.0-20260722.180737-40` (CI run
[29945235290](https://github.com/fuinorg/event-store-commons/actions/runs/29945235290) green).
The foundation the framework layers need is in place:

| | Delivered |
|---|---|
| **F1** | `org.fuin.esc.api.EscConnectionException` - one `instanceof` identifies every "store/database not reachable" failure |
| **F2** | gRPC connectivity statuses mapped to it; the `streamExists` bug fixed (a connectivity failure no longer reads as "stream does not exist") |
| **F3** | all 13 blocking gRPC calls bounded (5s default, configurable); `EventStoreCallTimeoutException` extends `EscConnectionException` |
| **F4** | JPA query + pessimistic-lock timeouts (`JpaTimeouts`, 5s default, `JpaEventStore.builder()`); transient JDBC/JPA failures mapped to `EscConnectionException` |

**Consumers can now classify with a single `instanceof EscConnectionException`.** `cqrs-4-java`
(`CqrsUtils.isTransientInfrastructureFailure`) still keys on the `java.util.concurrent.TimeoutException`
cause because it predates this release; it can be simplified once it builds against this version.

**Deliberately carried forward** (not blockers for the framework layers, but real gaps):

- ~~`ESGrpcEventStoreAsync` futures are still unbounded~~ - closed in Phase 0b.
- ~~`em.persist` / `em.remove` in `JpaCheckpointStore` and `JpaProjectionAdminEventStore` are not wrapped~~ -
  closed in Phase 0b.
- No config-property surface (`org.fuin.esc.eventstore.call-timeout-ms`); timeouts are set through the
  builder/constructor only, by design - no framework dependency in these modules.

---

## Phase 0b — DONE (2026-07-23)

The two gaps that Phase 0 carried forward are closed; the whole public surface of both stores is now bounded
and typed.

| | Delivered |
|---|---|
| **F3b** | `ESGrpcEventStoreAsync` is bounded exactly like the synchronous store - every returned future completes within `callTimeout` |
| **F4b** | All `EntityManager` access in `JpaCheckpointStore` and `JpaProjectionAdminEventStore` maps a connectivity failure to `EscConnectionException` |

**F3b** — new `GrpcCalls.within(future, timeout, operation)` bounds a client future: it works on a `copy()`
so the client's own future is untouched, fails the returned stage with `EventStoreCallTimeoutException`, and
cancels the original because nothing consumes it anymore. Applied to all 8 asynchronous calls
(`appendToStream`, `deleteStream`/`tombstoneStream`, `readEventsForward`, `readEventsBackward`,
`streamExists`, `streamState`, `readStreamMetaData`, `subscribeToStream`); `readEvent` is bounded through
`readEventsForward`. Configurable via the new `ESGrpcEventStoreAsync.Builder.callTimeout(Duration)`, default
`GrpcCalls.DEFAULT_CALL_TIMEOUT` (5 s) - same default and same builder name as the synchronous store.

`ESGrpcEventStoreSupport.mapException(..)` now returns an already-classified `EscConnectionException`
unchanged instead of wrapping it again, so a timeout keeps its operation name and elapsed duration; the
handlers in `streamExists`/`streamState`/`softDeleted` that do not go through `mapException` do the same.
A `subscribeToStream` that is established *after* its timeout fired is stopped right away - otherwise it
would deliver events to a listener whose subscription holder was never set.

**F4b** — `JpaUtils.execute(..)` got a `Runnable` overload for the void operations. `JpaCheckpointStore`
(`find`/`persist`/`remove`) and `JpaProjectionAdminEventStore` (`find`/`persist`/`remove`) route every
`EntityManager` call through it, so a broken connection surfaces as `EscConnectionException` there too - not
just on the writes, the reads had the same gap. `JpaStoreConnectionFailureTest` pins all six entry points
with an `EntityManager` proxy that fails like a closed connection.

NB `em.persist` usually only queues the insert until flush/commit, so the mapping fires where the provider
actually touches the database; the value is that *no* path out of these two classes can still emit a raw
`PersistenceException`.

**Behavioural changes to note when consuming this version:** `streamExists(..)` now *throws* on a
connectivity failure where it previously returned `false`, and paths that threw a plain `RuntimeException`
now throw `EscConnectionException`. A hard-deleted stream still answers `false` - that contract is pinned
by a test after it regressed once during F2.

---

## Phase 0 — Foundation (blocking; everything downstream needs this)

### F1. Typed transient/unavailable exception in `esc-api` — **S1/S2**
- [x] `org.fuin.esc.api.EscConnectionException extends RuntimeException` added (NOT final, so
      implementations can specialise it). A single `instanceof EscConnectionException` is enough for
      retry/CB predicates - no extra marker interface was introduced.
      `esgrpc`'s `EventStoreCallTimeoutException` now extends it, so the F3 timeout is covered by the same
      predicate.
- [x] Javadoc states the contract: transient = may be retried, but the outcome is unknown, so a write is
      only safe to retry with an expected version or deduplication; business exceptions never retry.

### F2. Classify & map connectivity errors in `esc-esgrpc` — **S1**
File: `esgrpc/src/main/java/org/fuin/esc/esgrpc/ESGrpcEventStoreSupport.java` (`mapException`, ~198–210).
- [x] New `ESGrpcEventStoreSupport.statusIsConnectivityProblem(..)` matches `UNAVAILABLE`,
      `DEADLINE_EXCEEDED`, `RESOURCE_EXHAUSTED`, `ABORTED`, `InterruptedException` and
      `EventStoreCallTimeoutException`; `mapException` returns `EscConnectionException` for those instead
      of the catch-all `RuntimeException`.
- [x] Business mappings unchanged - they are still checked first, so a deleted/not-found/wrong-version
      answer can never be reported as a connectivity problem.
- [x] **Bug fixed** in both `ESGrpcEventStore.streamExists` and `ESGrpcEventStoreAsync.streamExists`:
      only `NOT_FOUND` / `StreamNotFoundException` yields `false` (new helper
      `ESGrpcEventStoreSupport.statusIsNotFound(..)`); a connectivity failure now throws
      `EscConnectionException` instead of silently reporting "stream does not exist".

### F3. Per-call timeouts (deadlines) in `esc-esgrpc` — **S1**
- [x] All 13 blocking `.get()` calls are bounded: 8 in `ESGrpcEventStore` (append, delete, read
      forward/backward, streamExists, streamState, stream meta data) and 5 in
      `GrpcProjectionAdminEventStore` (getStatus, enable, disable, create, delete). They now go through
      `GrpcCalls.await(future, timeout, operation)`, which fails with `EventStoreCallTimeoutException`
      and cancels the future. Covered by `GrpcCallsTest` (a never-completed future fails in 200 ms).
      NB: the client-side wait is what actually helps here — the gRPC deadline does not fire when the work
      item is never scheduled at all, which was the observed hang (the client queued it behind a dead
      connection and nothing ever completed the future).
- [x] Configurable via `ESGrpcEventStore.Builder.callTimeout(Duration)` and a new
      `GrpcProjectionAdminEventStore(client, tenantContext, Duration)` constructor; no framework
      dependency. The old constructor delegates, so this is backwards compatible.
- [x] Default is **5 s** (`GrpcCalls.DEFAULT_CALL_TIMEOUT`), as suggested here.
- [ ] No `org.fuin.esc.eventstore.call-timeout-ms` config property: the timeout is builder/constructor
      only, so an application cannot change it without touching code.
- [x] `ESGrpcEventStoreAsync` futures are bounded too (Phase 0b): `GrpcCalls.within(..)` wraps all 8
      asynchronous calls, configurable via `ESGrpcEventStoreAsync.Builder.callTimeout(Duration)` with the
      same 5 s default. Covered by `GrpcCallsTest`.
- [x] Reconciled with F1: `EventStoreCallTimeoutException` (still in `esc-esgrpc`, it is a gRPC client
      detail) now **extends `org.fuin.esc.api.EscConnectionException`**, so one `instanceof` covers it.
- [ ] `cqrs-4-java` `CqrsUtils.isTransientInfrastructureFailure` still keys on the
      `java.util.concurrent.TimeoutException` cause; it can be simplified to `EscConnectionException` once
      it depends on an esc version that has it.

### F4. JDBC/JPA timeouts & typed mapping in `esc-jpa` — **S2**
Files: `jpa/.../JpaEventStore.java`, `jpa/.../AbstractJpaEventStore.java`.
- [x] All 7 query sites are bounded with `jakarta.persistence.query.timeout`, and the
      `PESSIMISTIC_WRITE` acquisition in `findAndLockJpaStream` additionally with
      `jakarta.persistence.lock.timeout` (helpers `JpaUtils.withQueryTimeout/withLockTimeout`).
- [x] Configurable through the new immutable `JpaTimeouts` (query + lock, default 5s each, rejects
      zero/negative because some providers read that as "no limit"). Injected via
      `AbstractJpaEventStore(em, ser, des, timeouts)` or the new `JpaEventStore.builder()`.
- [x] **`JpaEventStore.builder()` added** (mirrors `ESGrpcEventStore.Builder`) so the constructors do not
      have to keep growing; the existing constructors stay and default to `JpaTimeouts.DEFAULT`.
- [x] `JpaUtils.mapPersistenceException(..)` maps `QueryTimeoutException`, `LockTimeoutException`,
      `PessimisticLockException` and any `PersistenceException` with a JDBC/network cause
      (`SQLTransientException`, `SQLRecoverableException`, `SQLNonTransientConnectionException`,
      `IOException`) to `EscConnectionException`. Applied at the query execution points via
      `JpaUtils.execute(..)`.
- [x] `NoResultException` is deliberately left unchanged (callers map it to `EventNotFoundException`), and
      so is `OptimisticLockException` - a concurrency conflict is a business answer and must not be
      retried blindly. Both pinned by tests.
- [x] Pool `connection-timeout` expectation documented in the `JpaTimeouts` javadoc: obtaining a
      connection happens before any query runs and is owned by the application's datasource, so the worst
      case is pool wait + query timeout.
- [x] `JpaCheckpointStore` and `JpaProjectionAdminEventStore` route every `EntityManager` call - the reads
      as well as `em.persist` / `em.remove` - through the new `JpaUtils.execute(Runnable)` /
      `execute(Supplier)` (Phase 0b), so a connectivity failure surfaces as `EscConnectionException`
      instead of a raw `PersistenceException`. Covered by `JpaStoreConnectionFailureTest`.

---

## Phase 2 — Event store hardening — DONE (2026-07-23)

Reconnect and retry now share one `Backoff` and one set of rules about what may be repeated.

| | Delivered |
|---|---|
| **E1** | `Backoff` in `esc-api` + `ReconnectingSubscribableEventStore` decorator in `esc-spi` - store-agnostic auto-resubscribe with exponential backoff and jitter |
| **E2** | The retry contract for `appendToStream` documented on `WritableEventStore`, pinned by `AppendRetryIdempotencyTest` |
| **E3** | `GrpcCalls.awaitWithRetry(..)` replaces the hand-rolled `disableProjection` loop and now covers all five projection-admin calls |

### E1. Subscription auto-reconnect with backoff
- [x] New `org.fuin.esc.api.Backoff` (record): exponential delay, cap, jitter factor, attempt limit.
      `baseDelay(n)` is the deterministic schedule, `delay(n)` applies the jitter, `allowsAttempt(n)`
      answers the budget question. The cap keeps a long outage from turning into an even longer recovery;
      the jitter keeps several consumers of the same store from retrying in lockstep.
- [x] New `org.fuin.esc.spi.ReconnectingSubscribableEventStore` decorates **any**
      `SubscribableEventStoreAsync` (so `esc-mem` and `esc-esgrpc` alike) rather than living inside the gRPC
      store. That also makes it unit-testable against a fake store without a KurrentDB container -
      `ReconnectingSubscribableEventStoreTest` drives drops and unreachable-store phases on demand.
      Decision on the fork in the original task: **reconnect lives in the store layer**, and `cqrs-4-java`
      `ViewSubscriptions` can drop its own fixed-5 s loop once it builds against this version.
- [x] The `Subscription` handed to the consumer stays valid across reconnects (`ReconnectingSubscription`);
      `unsubscribeFromStream(..)` on it stops both the inner subscription and any pending reconnect.
- [x] Resume position: neither `CommonEvent` nor `Subscription` carries a stream position, so the decorator
      counts what the consumer accepted and resumes at `eventNumber + delivered`. Advancing only *after*
      `onEvent` returns makes delivery at-least-once - an event whose handler threw is redelivered.
- [x] Reconnect config exposed through `Backoff` (initial/max delay, multiplier, jitter, max attempts);
      the scheduler is supplied and owned by the caller, as with `ViewSubscriptions`.
- [ ] `SUBSCRIBE_TO_NEW_EVENTS` has no absolute anchor, so such a subscription is re-established as "new
      events" again and events written during the outage are not redelivered. Correct for a wake-up
      subscription (the catch-up pass reads them from its checkpoint), but it is not gap-free delivery.
- [ ] The **initial** subscribe is deliberately not retried - a failure there fails the returned future so
      the caller sees why its wiring did not come up. Revisit if a consumer wants "wait for the store to
      appear" at startup.

### E2. Idempotency notes for append retries
- [x] `WritableEventStore` (and the async twin) now document the contract: an `EscConnectionException`
      leaves the outcome unknown, so a retry with `ExpectedVersion.ANY` appends the events twice; only a
      concrete expected version makes the retry safe.
- [x] `AppendRetryIdempotencyTest` (in `esc-mem`) proves no double-append when the expected version is
      concrete, that `ANY` *does* duplicate, and that a genuine conflict is still rejected.
- [x] **Backends report the rejected repetition differently** and a retrying caller must handle both:
      `esc-mem` recognizes that the stream already ends with exactly those events and answers with the
      current version (a silent no-op), KurrentDB answers with `WrongExpectedVersionException`. Both keep
      the stream free of duplicates - that is the invariant, not the exception type. This surfaced while
      writing the test and is now stated in the javadoc.

### E3. Generalize the projection-admin retry model
- [x] New `GrpcCalls.awaitWithRetry(call, timeout, operation, overallTimeout, backoff, retryable)`: each
      attempt is bounded by the call timeout, the sequence by an overall budget (30 s), the delays come from
      `Backoff`. The hand-rolled `disableProjection` loop, its fixed 250 ms delay and `sleepQuietly` are
      gone.
- [x] Applied to all five calls, with the retry rule chosen per operation rather than one blanket policy:
      - `projectionExists`, `enableProjection`, `disableProjection` are idempotent, so they retry on any
        transient status (`statusIsConnectivityProblem`). `disableProjection` additionally keeps its
        `projectionNotReadyYet` rule and its short per-call deadline.
      - `createProjection` and `deleteProjection` are **not** safely repeatable: a second create answers
        "Conflict" (reported as `ProjectionAlreadyExistsException`) and a second delete answers "not found",
        so a repetition of a request that did arrive would be reported as a wrong business outcome. They
        retry only on the new, narrower `ESGrpcEventStoreSupport.statusIsUnreachable(..)`
        (`UNAVAILABLE` / `RESOURCE_EXHAUSTED` = the server never took the request).

---

## Phase 3 — Database hardening — DONE (2026-07-23)

| | Delivered |
|---|---|
| **D1** | The last unwrapped `EntityManager` calls in the event store itself are mapped - no path out of `esc-jpa` can still emit a raw `PersistenceException` |
| **D2** | A dropped `LISTEN` connection no longer silences the wake-up: it degrades to polling and reconnects with backoff |

### D1. Transient-classify and bound all DB access
- [x] Audited every `EntityManager` and native query path. The seven query executions were already bounded
      and mapped (F4); what was still bare were **three `em.find(JpaProjection.class, ..)` in
      `AbstractJpaEventStore`** (projection read forward/backward, `streamExists`) and **four
      `getEm().persist(..)` in `JpaEventStore`** - including the append path itself. All now go through
      `JpaUtils.execute(..)` (new `AbstractJpaEventStore.findProjection(..)` helper).
- [x] `streamExists(projectionStreamId)` used to answer `false` when the database was unreachable - the same
      trap the gRPC `streamExists` fell into in F2. It now throws `EscConnectionException`. Pinned by
      `JpaStoreConnectionFailureTest` (now 8 entry points).
- [x] `createQuery` / `createNativeQuery` and `entityExists(..)` need no wrapping: they build the query resp.
      read the in-memory metamodel and never touch the database.

### D2. `esc-pg` LISTEN/NOTIFY resilience
- [x] **Bug fixed:** on an `SQLException` from `getNotifications(..)` the loop logged, slept a slice and
      continued *without ever decrementing the poll budget or re-establishing anything*. On a dead connection
      that meant the wake-up never fired again - the projector went silent until the process restarted,
      even though the poll safety-net was supposed to carry it.
- [x] The loop now runs on explicit `nextPollAt` / `nextReconnectAt` deadlines. While the channel is down it
      keeps firing the callback every poll interval (the consumer's catch-up pass runs on its own connection
      and is unaffected), so the source degrades to exactly the plain poll it was documented to fall back to.
- [x] New `PgListenConnectionFactory` (deliberately not a `DataSource`: a listening connection is held for
      the lifetime of the source and would permanently remove one connection from the application's pool).
      Given one, the source re-opens the connection and re-issues `LISTEN` with `Backoff` (exponential +
      jitter, `Backoff.DEFAULT`, configurable). Once the backoff's attempts are used up it logs and continues
      as a plain poll rather than giving up entirely.
- [x] Every successful reconnect fires the callback immediately - notifications sent while nothing was
      listening are gone for good (PostgreSQL notifications are at-most-once and non-durable).
- [x] Connections the source opened are owned and closed by `close()`; a caller-supplied `Connection` is
      still never closed. The old constructor keeps working unchanged and simply has nothing to reconnect
      with, so it stays in poll-only mode after a drop.
- [x] The **first** connection is deliberately not retried: a store unreachable at wiring time is a startup
      problem the caller should see (same rule as the `ReconnectingSubscribableEventStore` in E1).
- [x] Covered by three new Testcontainer tests: wake-ups continue after the connection is closed under the
      listening thread, a reconnect opens a fresh connection and the notification latency comes back
      (proven by an `INSERT` after the reconnect), and a factory that cannot connect fails `start(..)`.

---

## Phase 5 — Crypto store decorator — DONE (2026-07-23)

A vault outage is now a typed, transient failure that can neither be mistaken for data nor hang the caller.
The *policy* still lives in `ddd-4-java` / the app (see
[ddd-4-java tasks](https://github.com/fuinorg/ddd-4-java/blob/develop/resilience-tasks.md) S5); this layer
only makes the failure classifiable.

- [x] New `EscEncryptionConnectionException extends org.fuin.esc.api.EscConnectionException`. Extending the
      general type rather than `EscEncryptionException` is the point: a store whose key service is
      unavailable *is* unavailable, so the single `instanceof EscConnectionException` predicate from F1 keeps
      working, and the dedicated type is only there for a consumer that wants to tell "the vault is down"
      from "the store is down". (`EscEncryptionException` is `final`, and it means the opposite - a definite
      answer that must never be retried.)
- [x] New package-private `KeyServiceCalls` wraps every call into the key service. Classification walks the
      cause chain for `IOException` / `TimeoutException`, which is what an HTTP client reports for an
      unreachable, refusing or timed-out vault; a client that can classify better should throw an
      `EscConnectionException` itself and is passed through untouched. The three checked exceptions of
      `EncryptedDataService` are definite answers and pass through with their original type.
- [x] Applied to `encrypt`, `decrypt` and `KeyIdResolver.getKeyId(..)` - the shipped resolvers are local, but
      a custom one may have to ask a service which key applies, and it sits on the append path.
- [x] **A transient failure never degrades to "undecryptable".** `failOnUndecryptable(false)` returns the
      ciphertext wrapper for an event whose key or key version is *permanently* gone. Had a vault outage
      been routed into that path, a caller would have received the wrapper and taken it for data. The
      transient exception is unchecked and deliberately not in that catch block; pinned by a test, and the
      catch carries a comment saying why nothing may be added to it.
- [x] Optional `Builder.keyServiceTimeout(Duration)` bounds how long a single key service call may block the
      appending or reading thread; `keyServiceTimeout(Duration, ExecutorService)` takes a caller-owned
      executor instead of the daemon pool the store otherwise creates and shuts down in `close()`.
- [ ] The timeout is **opt-in and does not abort the request**: the call runs on an executor and keeps
      running after the timeout (nothing can interrupt a socket read from outside). What is bounded is the
      wait of the thread that appends or reads - the same reasoning as F3. The key service client's own
      connect/read timeout remains the better primary bound and is documented as such.

---

## Testing — DONE (2026-07-23)

Every guarantee the earlier phases claim is now checked against a **real** store and a **real** database,
with the failure injected rather than mocked.

### `ESGrpcFaultInjectionIT` (esc-esgrpc, 5 tests)
- [x] Fault injection is done with a small in-process TCP forwarder (`FaultInjectingProxy`) instead of
      Testcontainer `pause()`/`stop()` or Toxiproxy. The KurrentDB these ITs run against is started by the
      docker-maven-plugin **on the host network**, so there is no container handle to pause; a socket
      forwarder needs no container engine at all and gives exact control. gRPC is HTTP/2 over TCP, so
      forwarding bytes is enough. Modes: `forward()`, `blackhole()` (accept and swallow - the call is sent
      but never answered), `cut()` (drop open connections, refuse new ones).
- [x] **Calls time out, they do not hang.** A blackholed `appendToStream` fails with
      `EventStoreCallTimeoutException`, and the test asserts the elapsed time is **at least** the configured
      call timeout - proving it really waited for an answer and was cut short by F3, rather than failing
      early for some unrelated reason.
- [x] **Failures are typed.** A cut connection makes `readEventsForward` throw `EscConnectionException`, not
      a bare `RuntimeException`.
- [x] **`streamExists(..)` never turns "no answer" into "no stream"** (the F2 bug): on a cut connection it
      throws instead of answering `false`, and a companion test keeps the business answer honest - a stream
      that was really never written still answers `false`.
- [x] **The store recovers** once the connection is restored: reads and writes both work again after
      `forward()` (with an awaitility loop, because the gRPC client backs off before reconnecting).

### `JpaFaultInjectionIT` (esc-jpa, 2 tests, PostgreSQL via Testcontainers)
- [x] **Stall:** one transaction holds the `PESSIMISTIC_WRITE` lock on the stream row while another appends
      to the same stream. The blocked append fails with `EscConnectionException` after the configured lock
      timeout, and the test asserts it waited at least that long - which also **confirms Hibernate and the
      PostgreSQL dialect actually honour `jakarta.persistence.lock.timeout`**. That was the open question
      behind F4: setting the hint is not the same as the backend enforcing it.
      NB the stream row has to be **committed** before the contention starts - an uncommitted `INSERT` is
      invisible to the other transaction, which then simply creates its own row and never contends. An
      earlier version of this test passed without proving anything for exactly that reason.
- [x] **Drop:** the database container is stopped under an open connection; the next read fails with
      `EscConnectionException` rather than a raw `PersistenceException`. Uses `hbm2ddl.auto=create` (not
      `create-drop`), because a drop at factory shutdown would otherwise fail the test in its teardown.
- [x] Both are wrapped in `assertTimeoutPreemptively`, so a regression that makes the store wait forever
      fails the build instead of hanging it.

Still open here: the `FaultInjectingProxy` lives in the esgrpc test sources. If a third module ever needs
it, hoist it into a shared test-jar rather than copying it.

---

## Optional — programmatic Resilience4j *inside* esc (only if maintainers want turnkey resilience here)
- [ ] If the neutral libs should ship retry/CB/timeout themselves (rather than leaving policy to the app),
      add the framework-agnostic **Resilience4j core** modules (`resilience4j-retry`, `-circuitbreaker`,
      `-timelimiter`, `-bulkhead` — plain Java, no Spring/CDI) and decorate the `ESGrpcEventStore*` calls
      via a `Decorators.ofSupplier(...)` wrapper keyed on `EscConnectionException`. Keep it behind a
      builder flag so consumers can opt out. **Default remains: no resilience framework in esc** — policy
      lives in cqrs-4-java's Quarkus/Spring modules and the apps.
