# Micrometer Tasks — event-store-commons

Part of the cross-repo [Micrometer Instrumentation Roadmap](https://github.com/fuinorg/ddd-cqrs-4-java-example/blob/develop/micrometer-roadmap.md).
This is **upstream #1** — release before ddd-4-java and cqrs-4-java.

Micrometer stays **optional**: neutral modules see only the micrometer-free `EscMetrics` facade
(default `NOOP`); all `io.micrometer.*` code lives in the new opt-in `micrometer` module. Meter at
**exactly one layer** — wrap the concrete backend only, never a `Delegating*` bridge or
`EncryptingEventStore` (see the roadmap's double-counting rule).

## Phase 0 — Facade & module scaffolding

- [ ] Add micrometer-free facade `EscMetrics` (interface + `NOOP` singleton) in the `api` module,
      package `org.fuin.esc.api`. Methods: `recordTime(String name, long nanos, String... tags)`,
      `increment(String name, String... tags)`, `record(String name, double amount, String... tags)`.
- [ ] Import `io.micrometer:micrometer-bom` in the parent
      [pom.xml](https://github.com/fuinorg/event-store-commons/blob/develop/pom.xml)
      `<dependencyManagement>` (next to the existing BOM imports). Neutral modules get **no**
      micrometer dependency.
- [ ] Create new module `event-store-commons/micrometer` (`esc-micrometer`): depends on
      `micrometer-core` (normal scope — whole module is opt-in) + `esc-api`. Register it in the
      [bom/pom.xml](https://github.com/fuinorg/event-store-commons/blob/develop/bom/pom.xml) and
      parent `<modules>`.
- [ ] Add an ArchUnit rule forbidding `io.micrometer` imports outside the `micrometer` module.

## Phase 1 — Decorators (⭐ 20% set: the gRPC store + crypto)

In the new `micrometer` module:

- [ ] ⭐ `MeteredEventStoreAsync implements EventStoreAsync` — wrap a delegate, `backend` tag from
      the constructor. Times each op → `esc.eventstore.append|read|delete|exists|state` (tags
      `backend`, `operation`, `outcome`); classify failed futures → `esc.eventstore.errors`
      (tags `backend`, `exception`), deriving the outcome from the exception mapped by
      [`ESGrpcEventStoreSupport.mapException`](https://github.com/fuinorg/event-store-commons/blob/develop/esgrpc/src/main/java/org/fuin/esc/esgrpc/ESGrpcEventStoreSupport.java).
      Wrap points: [`ESGrpcEventStoreAsync`](https://github.com/fuinorg/event-store-commons/blob/develop/esgrpc/src/main/java/org/fuin/esc/esgrpc/ESGrpcEventStoreAsync.java)
      and [`InMemoryEventStoreAsync`](https://github.com/fuinorg/event-store-commons/blob/develop/mem/src/main/java/org/fuin/esc/mem/InMemoryEventStoreAsync.java).
- [ ] `MeteredEventStore implements EventStore` — wrap the sync-native
      [`JpaEventStore`](https://github.com/fuinorg/event-store-commons/blob/develop/jpa/src/main/java/org/fuin/esc/jpa/JpaEventStore.java)
      (`backend=jpa`).
- [ ] `MicrometerEscMetrics implements EscMetrics` — adapter over a `MeterRegistry` for the
      intra-method seams below.

## Phase 2 — Facade seams inside neutral modules (default `EscMetrics.NOOP`)

- [ ] ⭐ [`EncryptingEventStore`](https://github.com/fuinorg/event-store-commons/blob/develop/crypto/src/main/java/org/fuin/esc/crypto/EncryptingEventStore.java):
      add `Builder.metrics(EscMetrics)`; time `encrypt(...)`/`decrypt(...)` (the external
      key-service round-trip) → `esc.crypto.encrypt|decrypt` (tag `outcome`); increment
      `esc.crypto.undecryptable` in the `failOnUndecryptable == false` catch branch (~line 262).
- [ ] [`JpaEventStore`](https://github.com/fuinorg/event-store-commons/blob/develop/jpa/src/main/java/org/fuin/esc/jpa/JpaEventStore.java)
      + [`AbstractJpaEventStore`](https://github.com/fuinorg/event-store-commons/blob/develop/jpa/src/main/java/org/fuin/esc/jpa/AbstractJpaEventStore.java):
      optional `EscMetrics`; time `esc.jpa.lock` around `findAndLockJpaStream` (`PESSIMISTIC_WRITE`)
      and the read queries.
- [ ] [`CatchupSubscription`](https://github.com/fuinorg/event-store-commons/blob/develop/spi/src/main/java/org/fuin/esc/spi/CatchupSubscription.java):
      optional `EscMetrics`; time `catchUp()` per pass → `esc.catchup.pass`; count
      `esc.catchup.events`; `esc.catchup.errors` in the `catchUpGuarded` catch; `esc.catchup.lag`
      gauge (checkpoint vs head).
- [ ] [`PgListenNotifyWakeupSource`](https://github.com/fuinorg/event-store-commons/blob/develop/pg/src/main/java/org/fuin/esc/pg/PgListenNotifyWakeupSource.java):
      optional `EscMetrics`; `esc.pg.wakeup` Counter (tag `source=notify|poll`),
      `esc.pg.listen.errors` Counter on the `SQLException` path.

## Phase 3 — Serialization & admin (lower priority)

- [ ] `MeteredSerializer` / `MeteredSerDeserializer` in the `micrometer` module → `esc.serde.serialize|deserialize`
      Timer (tag `contentType`) + `esc.serde.payload.bytes` DistributionSummary. Per-event — gate
      behind the opt-in module.
- [ ] [`GrpcProjectionAdminEventStore`](https://github.com/fuinorg/event-store-commons/blob/develop/esgrpc/src/main/java/org/fuin/esc/esgrpc/GrpcProjectionAdminEventStore.java)
      `disableProjection` retry loop → optional `EscMetrics` retry counter.

## Tests

- [ ] One `SimpleMeterRegistry` test per decorator/adapter, mirroring the cqrs `*MetricsTest`
      pattern. Use the real `InMemoryEventStore` as the no-network delegate; a fake
      `EncryptedDataService` to exercise the `esc.crypto.undecryptable` branch.
- [ ] Double-counting test: metered backend behind `DelegatingSyncEventStore` + `EncryptingEventStore`
      → assert the store timer fires exactly once per logical op.
- [ ] Zero-impact test: neutral classes with `EscMetrics.NOOP` behave identically; ArchUnit
      confirms no `io.micrometer` leak into neutral modules.
