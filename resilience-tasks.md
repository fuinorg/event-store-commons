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

## Phase 0 — Foundation (blocking; everything downstream needs this)

### F1. Typed transient/unavailable exception in `esc-api` — **S1/S2**
- [ ] Add `org.fuin.esc.api.EscConnectionException extends RuntimeException` (marker for "store not
      reachable / transient infrastructure failure"), clearly distinct from the existing *business*
      exceptions (`WrongExpectedVersionException`, `StreamNotFoundException`, `StreamDeletedException`,
      `StreamAlreadyExistsException`, `StreamReadOnlyException`, `ProjectionAlreadyExistsException`,
      `EventNotFoundException`). Consider a common base/marker interface so retry/CB predicates can do a
      single `instanceof`.
- [ ] Javadoc the contract: transient = safe to retry (subject to idempotency); business = never retry.

### F2. Classify & map connectivity errors in `esc-esgrpc` — **S1**
File: `esgrpc/src/main/java/org/fuin/esc/esgrpc/ESGrpcEventStoreSupport.java` (`mapException`, ~198–210).
- [ ] Map `io.grpc.StatusRuntimeException` with `UNAVAILABLE`, `DEADLINE_EXCEEDED`, `RESOURCE_EXHAUSTED`,
      `ABORTED` (and `InterruptedException`) to `EscConnectionException` instead of the catch-all
      `new RuntimeException("Error executing event store operation", cause)`.
- [ ] Keep business mappings unchanged (`WrongExpectedVersionException`, deleted, not-found).
- [ ] **Bug fix:** `ESGrpcEventStore.streamExists` (~253–275) and `ESGrpcEventStoreAsync.streamExists`
      (~235–253) currently return `false` for **any** `StatusRuntimeException` — a connectivity failure is
      silently reported as "stream does not exist". Only treat the not-found status as `false`; rethrow a
      connectivity `StatusRuntimeException` as `EscConnectionException`.

### F3. Per-call timeouts (deadlines) in `esc-esgrpc` — **S1**
- [ ] Give every blocking `.get()` in `ESGrpcEventStore` a bounded wait / gRPC deadline (append, delete,
      read forward/backward, streamExists, streamState). Today they block indefinitely. Model on
      `GrpcProjectionAdminEventStore.disableProjection`'s `options.deadline(DISABLE_CALL_DEADLINE_MILLIS)`
      + `while` loop bounded by `DISABLE_TIMEOUT_MILLIS`.
- [ ] Make the deadline configurable (default e.g. 5 s) via a small config object / builder param
      (`org.fuin.esc.eventstore.call-timeout-ms`); no framework dependency.
- [ ] Do the same for `ESGrpcEventStoreAsync` futures (`orTimeout(...)` / `completeOnTimeout(...)` mapping
      to `EscConnectionException`).

### F4. JDBC/JPA timeouts & typed mapping in `esc-jpa` — **S2**
Files: `jpa/.../JpaEventStore.java`, `jpa/.../AbstractJpaEventStore.java`.
- [ ] Set query/lock timeouts on the JPA operations: `query.setHint("jakarta.persistence.query.timeout", ms)`
      and a lock timeout for the `PESSIMISTIC_WRITE` acquisition in `findAndLockJpaStream` (avoid
      unbounded lock waits).
- [ ] Map transient `PersistenceException`/`LockTimeoutException`/`QueryTimeoutException`/JDBC connectivity
      failures to `EscConnectionException` (leave `NoResultException → EventNotFoundException` as-is).
- [ ] Document the pool `connection-timeout` expectation (owned by the app's datasource, but reference it
      here so retry has a bounded worst case).

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
