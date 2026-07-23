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

## Phase 2 — Event store hardening (`esc-esgrpc`) — **S1**

### E1. Subscription auto-reconnect with backoff
File: `esgrpc/.../ESGrpcEventStoreAsync.java` (`subscribeToStream`, ~294–338; `onDrop`/`onCancelled`).
- [ ] Today a dropped subscription just delivers an error via `onDrop`/`onCancelled` (no reconnect).
      Add optional auto-resubscribe with **exponential backoff + jitter** (resume from the last delivered
      position). This complements `cqrs-4-java` `ViewSubscriptions` (fixed 5 s) — decide whether reconnect
      lives in the store (reusable for all consumers) or stays in cqrs4j; generalize the backoff either
      way.
- [ ] Expose reconnect config (initial/max backoff, max attempts, jitter).

### E2. Idempotency notes for append retries
- [ ] Document/guard: `appendToStream` retries are only safe on `EscConnectionException` raised *before*
      the server acknowledges; rely on `ExpectedVersion` to reject duplicates. Add a test proving no
      double-append under retry.

### E3. Generalize the projection-admin retry model
File: `esgrpc/.../GrpcProjectionAdminEventStore.java`.
- [ ] Extract the `disableProjection` deadline+backoff+`projectionNotReadyYet` pattern into a small reusable
      helper and apply the same bounded-retry to `enableProjection`, `createProjection`, `deleteProjection`,
      `projectionExists` where a transient status warrants it.

---

## Phase 3 — Database hardening (`esc-jpa`, `esc-pg`) — **S2**

### D1. Transient-classify and bound all DB access
- [ ] Ensure every `EntityManager`/native query path in `AbstractJpaEventStore` (append, read, projection
      position) has a timeout and maps transient failures to `EscConnectionException`.
### D2. `esc-pg` LISTEN/NOTIFY resilience
File: `pg/.../PgListenNotifyWakeupSource.java`.
- [ ] The JDBC `LISTEN` connection is long-lived; add reconnect-with-backoff if the notify connection drops,
      and a bounded poll fallback so wake-ups degrade to polling when NOTIFY is unavailable.

---

## Phase 5 — Crypto store decorator (`esc-crypto`) — **S5**
File: `crypto/.../EncryptingEventStore.java` (+ `KeyIdResolver`, `EncryptedDataFactory`).
- [ ] The encrypt/decrypt decorator calls the external key service; a vault outage must not corrupt or hang
      the store. Ensure key-service failures surface as a typed transient exception (or `EscEncryptionException`
      subtype) so the app layer can apply retry/CB/fallback (the actual policy is applied in `ddd-4-java` /
      the app — see [ddd-4-java tasks](https://github.com/fuinorg/ddd-4-java/blob/develop/resilience-tasks.md) S5). Bound any key-service call
      with a timeout here.

---

## Testing (esc-test TCK + esgrpc ITs)
- [ ] Add fault-injection ITs: with the KurrentDB/EventStoreDB Testcontainer, `pause()`/`stop()` mid-call
      (or Toxiproxy latency/blackhole) and assert: calls **time out** (not hang), `EscConnectionException`
      is thrown (not a bare `RuntimeException`, not a false `streamExists`), and the store recovers when the
      container returns.
- [ ] JPA transient-failure test: drop/stall the DB connection and assert `EscConnectionException` + timeout.

---

## Optional — programmatic Resilience4j *inside* esc (only if maintainers want turnkey resilience here)
- [ ] If the neutral libs should ship retry/CB/timeout themselves (rather than leaving policy to the app),
      add the framework-agnostic **Resilience4j core** modules (`resilience4j-retry`, `-circuitbreaker`,
      `-timelimiter`, `-bulkhead` — plain Java, no Spring/CDI) and decorate the `ESGrpcEventStore*` calls
      via a `Decorators.ofSupplier(...)` wrapper keyed on `EscConnectionException`. Keep it behind a
      builder flag so consumers can opt out. **Default remains: no resilience framework in esc** — policy
      lives in cqrs-4-java's Quarkus/Spring modules and the apps.
