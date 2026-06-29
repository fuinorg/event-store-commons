# event-store-commons (esc) — Architecture Review (Event Sourcing focus)

**Scope:** the `esc` event-store abstraction and its backends/serialization (`org.fuin.esc`, `esc-parent` v0.10.0-SNAPSHOT, JDK 17).
**Method:** static review of all modules (`api`, `spi`, `mem`, `esgrpc`, `jpa`, `crypto`, `jackson`/`jaxb`/`jsonb`, `client`, `test`, `bom`) and the shared test suite.
**Companions:** [ddd-4-java/arch-review.md](https://github.com/fuinorg/ddd-4-java/tree/develop/arch-review.md) (the ES engine that calls this) and [cqrs-4-java/arch-review.md](https://github.com/fuinorg/cqrs-4-java/tree/develop/arch-review.md) (CQRS/projections on top). 
This is the **lowest layer**, so several cross-cutting fixes referenced by the other two are *owned here*.

---

## 1. What this project is

`esc` is a **backend-neutral event-store abstraction**: one API (`EventStore` / `EventStoreAsync`) with pluggable implementations and pluggable serialization. It is the foundation both `ddd-4-java` (aggregate persistence) and `cqrs-4-java` (projections) depend on.

```
api    → interfaces + value objects (EventStore, CommonEvent, StreamId, ExpectedVersion, serialization contracts)
spi    → impl helpers (AbstractReadableEventStore, DelegatingSyncEventStore, EscSpiUtils, SerializedData)
mem    → in-memory backend (catch-up subscriptions; volatile) — the test/dev store
esgrpc → EventStoreDB / Kurrent gRPC backend (persistent + catch-up subs, server-side JS projections)
jpa    → relational backend (append/read only)
crypto → EncryptingEventStore decorator (AES; data encrypted, metadata optional)
jackson / jsonb / jaxb → Serializer/Deserializer implementations
test   → shared TCK (compliance suite + Cucumber) run against each backend
```

---

## 2. Event-sourcing design as built

- **Interface segregation, async-first.** `EventStoreBasics` → `Readable`/`Writable` → `EventStore`; mirrored async (`…Async`, `CompletableFuture`) with `DelegatingSyncEventStore` providing the blocking facade. `SubscribableEventStoreAsync` and `ProjectionAdminEventStore` are separate capabilities a backend may or may not implement.
- **Write model.** `appendToStream(streamId, expectedVersion, events…) → newVersion`; `ExpectedVersion` = `ANY(-2)` / `NO_OR_EMPTY_STREAM(-1)` / explicit. Mismatch → `WrongExpectedVersionException(expected, actual)`. **Idempotent append**: on a version clash whose trailing events equal the incoming batch, the existing version is returned instead of throwing (`EscSpiUtils.eventsEqual`). **Soft vs hard delete** + `StreamState` (`ACTIVE`/`SOFT_DELETED`/`HARD_DELETED`); soft-deleted streams can be recreated.
- **Event envelope.** `CommonEvent` = `id` (UUID) + `dataType` (`TypeName`) + nullable `tenantId` + `data` + nullable `metaType`/`meta`. Stream ids: `SimpleStreamId`, `ProjectionStreamId` (`isProjection()=true`, append → `StreamReadOnlyException`), `TenantStreamId` (tenant-prefixed).
- **Read model.** `readEventsForward/Backward` (paged `StreamEventsSlice`), `readEvent`, and `readAllEventsForward(streamId, start, chunkSize, handler)` — the call `cqrs-4-java` polls for projections.
- **Serialization (version-aware).** `CommonEvent` data is stored with a `SerializedDataType` + an `EnhancedMimeType` that carries **`version`** and **`encoding`** parameters. `DeserializerRegistry.getDeserializer(type, mimeType)` is **keyed by `(type, version)`** (the registry key's equality includes the mime type), so a distinct `Deserializer` can be registered per event version. `SerializedDataTypeRegistry` maps `SerializedDataType → Class`. Implementations in `jackson`/`jsonb`/`jaxb`.
- **Crypto.** `EncryptingEventStore` decorates any backend: serialize → AES-encrypt → wrap → append; reverse on read. Data encrypted by default, metadata left plaintext for routing (optional encrypt-meta). Key selection via `KeyIdResolver`.
- **Backends differ by capability** (important):

  | Backend | append/read | subscribe | projections | persistence |
  |---|---|---|---|---|
  | `mem` | ✓ | ✓ catch-up+live | ✗ | volatile |
  | `esgrpc` (EventStoreDB) | ✓ | ✓ catch-up **and persistent** | ✓ (server-side JS, by type) | server |
  | `jpa` | ✓ | ✗ | ✗ | RDBMS |

---

## 3. Strengths

1. **Clean, segregated, async-first API** with a sync adapter — implementations opt into `Subscribable`/`ProjectionAdmin` only if they can.
2. **Complete write semantics:** optimistic concurrency, idempotent append, soft/hard delete, explicit `StreamState`. This is a correct, well-thought event-store contract.
3. **Version-aware serialization substrate already exists** — `EnhancedMimeType.version` + `(type, version)`-keyed deserializer registries + `SerializedDataTypeRegistry`. This is exactly what upper layers need to evolve events; it's a real asset.
4. **Pluggable backends behind one contract, verified by a shared TCK** (the `test` module runs the same suite against `mem`/`jpa`/`esgrpc`) — strong design discipline.
5. **Transparent encryption decorator** — composes with any backend and enables crypto-shredding / GDPR "forget the key".
6. **Multi-tenancy** at the stream-id level.
7. Engineering rigor: Error Prone + NullAway, `@ThreadSafe` discipline, Jandex, dependency analysis.

---

## 4. Gaps & risks (event-sourcing lens)

> This is the layer that *owns* the cross-cutting versioning/projection fixes referenced by the other two reviews.

### 4.1 Upcasting chain not wired — the keystone gap
Version-aware **deserialization** is supported, but there is **no automatic version-conversion (upcasting) pipeline**. The generic `Converter<S,T>` interface exists yet is **unused** — there is no `ConverterRegistry` and nothing in the read path composes `v1→v2→…→vN` to hand consumers a single *current* representation. So today each version deserializes to its own type and every consumer (aggregate replay, projections, command handlers) must branch on version. This is the single highest-leverage gap, and it is felt in `ddd-4-java` §4.1 and `cqrs-4-java` §4.3.

### 4.2 The projection model is effectively EventStoreDB-specific
`ProjectionAdminEventStore` creates **server-side JS by-type projections** — implemented only by `esgrpc`. `mem` and `jpa` provide **no** projections. Because `cqrs-4-java`'s view engine builds a projection stream via `ProjectionAdminEventStore` and then polls `readAllEventsForward` on it, **the read side is implicitly coupled to EventStoreDB**. Moving to the `jpa` backend silently loses the projection mechanism the upper layer relies on — a portability cliff that isn't surfaced by the API.

### 4.3 Subscriptions/checkpoints are backend-asymmetric and non-portable
Catch-up subscriptions exist on `mem` and `esgrpc`; **`jpa` has none**. Durable/persistent subscription *position* is an EventStoreDB feature only — the API's `Subscription` is an in-process handle with no portable checkpoint store. So any catch-up consumer on a non-EventStoreDB backend must invent its own position tracking (which is exactly what `cqrs-4-java` does in a DB table). The abstraction doesn't offer a backend-neutral catch-up + checkpoint primitive.

**Why `jpa` can't simply "push" — it depends on the RDBMS:**
- **PostgreSQL** has native async pub/sub (`LISTEN`/`NOTIFY`, typically fired by an `INSERT` trigger, delivered on commit). But it is **at-most-once and not durable** (missed if no listener is connected), payload ≤ ~8 KB, and pgjdbc surfaces it only by polling `getNotifications(timeout)` on a dedicated connection (r2dbc-postgresql exposes a real reactive stream). So `NOTIFY` is a **latency wake-up**, not a substitute for a checkpoint.
- **MariaDB / MySQL** have **no `LISTEN`/`NOTIFY` equivalent** at all — the only async change feed is **CDC from the binlog** (Debezium/Maxwell/Canal), which is separate infrastructure.
- **Durable push** on either engine ultimately means **CDC** (Postgres logical decoding / MySQL-MariaDB binlog).

Consequence: the only mechanism portable across both databases is **checkpoint + catch-up read** (poll from the stored position). Database push is an *optimization* (Postgres) or *extra infra* (MariaDB CDC), never the portable baseline — which is what P2 codifies.

### 4.4 Capability differences are implicit
A backend's support for subscriptions/projections is only discoverable by which interfaces it implements (or by failure). There's no capability descriptor, so a consumer can wire against `jpa` and only discover at runtime that projections/subscriptions are absent.

### 4.5 No snapshot SPI
`esc` has no `Snapshot*` type. Reasonable as a layering choice, but it means `ddd-4-java` can only do **in-memory** partial replay via its `AggregateCache` seam (cold after restart) — there's no place to plug a **durable** snapshot through the store. (See `ddd-4-java/arch-review.md` §4.2.)

### 4.6 No negotiation/down-cast helper
`EnhancedMimeType.version` is the perfect substrate for content negotiation (a consumer asks for version X), but there's no helper to negotiate/serve/down-convert a stored event to a requested older version for integration consumers.

---

## 5. Recommendations — where to evolve, and how

### P1 — Wire an upcaster/converter chain into the read path (keystone; benefits all consumers)
Connect the two things that already exist (version-keyed deserialization + `Converter`):
- Add a **`ConverterRegistry`** keyed by `(SerializedDataType, fromVersion)` and compose registered `Converter`s into a chain that lifts any stored version to the **latest** in-memory type. Support skip-converters and the **reverse direction** (`vN→v1`) for negotiation/integration.
- Apply it centrally in `spi` (`EscSpiUtils`/`AbstractReadableEventStore`) right after `Deserializer.unmarshal(...)`, so **every backend** (mem/jpa/esgrpc) and every consumer (ddd-4-java replay, cqrs projections) gets upcast events for free.
- Document the rule: *a new "version" must be convertible from the old; otherwise it's a new event type.*

### P2 — A portable, backend-neutral catch-up + checkpoint SPI (decouple read side from EventStoreDB)
Push the projection/follow mechanism down so it doesn't require EventStoreDB:
- Provide an `spi` **catch-up driver**: `readAllEventsForward` (or a by-type filter) + a pluggable **`CheckpointStore`** (the durable position), implemented once and usable over `jpa`/`mem`/`esgrpc` alike.
- This is exactly what `cqrs-4-java` re-implements on top (DB checkpoint + cron poll); pulling it into `spi` removes the EventStoreDB coupling, lets the `jpa` backend drive read models, and gives every consumer the same primitive. (Connects to `cqrs-4-java` §P1/§P2.)

**Implement it as a three-rung ladder** (a `WakeupSource` SPI lets each backend plug in its best signal without changing the driver; see §4.3):
1. **Checkpoint catch-up (portable, required).** Poll `readEventsForward` from the persisted position. Works on MariaDB *and* PostgreSQL; correctness never depends on a push signal.
2. **Optional low-latency wake-up.** On **PostgreSQL**, a `LISTEN/NOTIFY` `WakeupSource` (INSERT trigger → notify) replaces the fixed poll interval with "read now" — cutting latency while the checkpoint still guarantees no missed events. MariaDB has no equivalent, so it stays on interval polling.
3. **Optional durable push.** A **CDC** `WakeupSource` (Postgres logical decoding / MariaDB-MySQL binlog via Debezium — Apache-2.0, production-grade; MySQL/MariaDB + Postgres are its most mature connectors) for those who want a real push feed — more capable, heavier infra, arguably beyond esc's core scope.
   - **HA caveat (embedded engine):** the in-process **Debezium Engine** avoids a Kafka dependency but gives up Debezium's own HA — it is **at-least-once, single-instance, and you own offset persistence**. Running it on >1 app instance needs a **distributed lease / leader election** (the same gap flagged in `cqrs-4-java/arch-review.md` §4.1 / §P1), and consumers must be idempotent. Kafka-Connect / Debezium-Server mode keeps the managed offset+failover but reintroduces that infrastructure.

### P3 — Make backend capabilities explicit
Add a capability descriptor (e.g. `EventStoreCapabilities { subscriptions, projections, persistentSubscriptions, hardDelete }`) or `supports(...)` checks, so consumers fail fast / adapt instead of discovering gaps at runtime. Document the EventStoreDB-only features clearly.

### P4 — Optional durable snapshot SPI
Define a small `SnapshotStore` / `Snapshotable` contract in `api`/`spi` (even with only `mem` + `jpa` impls) so `ddd-4-java` can persist **durable** snapshots through `esc` rather than relying solely on its in-memory `AggregateCache`. Keep it opt-in and rebuild-not-upgrade on snapshot schema change. (Pairs with `ddd-4-java` §P2.)

### P5 — Content-negotiation / down-cast utility
Offer a thin negotiation helper over `EnhancedMimeType.version` + the P1 converter chain: given a requested version, serve the stored event converted to it. Useful for HTTP/integration consumers and external models.

### P6 — Lift persistent-subscription checkpointing into the API (or document it)
Either generalize EventStoreDB's persistent-subscription position into a backend-neutral interface (backed by P2's `CheckpointStore` for other backends) or explicitly document that durable subscription position is store-specific.

---

## 6. Roadmap snapshot

| Priority | Item | Effort | Risk if skipped |
|---|---|---|---|
| **P1** | Upcaster/`Converter` chain wired into read path (spi) | M | High — events can't evolve without per-consumer version branching |
| **P2** | Portable catch-up + `CheckpointStore` SPI | M–L | High — read side locked to EventStoreDB; jpa unusable for projections |
| **P3** | Explicit backend capability descriptor | S | Medium — silent runtime gaps when switching backends |
| **P4** | Optional durable snapshot SPI | M | Low–Med — only in-memory partial replay upstream |
| **P5** | Negotiation / down-cast helper | S | Low — integration/external-model ergonomics |
| **P6** | Backend-neutral persistent-subscription position | M | Low–Med — checkpointing re-invented per consumer |

**Already strong, keep as-is:** the segregated async-first API, the write semantics (optimistic concurrency + idempotent append + soft/hard delete + `StreamState`), the **version-aware serialization substrate**, the pluggable-backend + shared-TCK design, the encryption decorator, and multi-tenancy.

---
*Generated as an architectural review; no code was modified. Paths reference `org.fuin.esc` modules under this repository. Capability notes (`mem`/`jpa`/`esgrpc` differences) reflect the 0.10.0 sources.*
