# esc-crypto

Transparent encryption of event data by wrapping any other event store implementation.

The [EncryptingEventStore](src/main/java/org/fuin/esc/crypto/EncryptingEventStore.java) is a decorator that implements
the [EventStore](../api/src/main/java/org/fuin/esc/api/EventStore.java) interface and delegates to another event store.
On **append** it serializes the event data, encrypts it and wraps it into an
[EncryptedData](https://github.com/fuinorg/objects4j/blob/master/crypto/src/main/java/org/fuin/objects4j/crypto/EncryptedData.java)
that the underlying store persists like any other event. On **read** it detects the wrapper, decrypts it and reconstructs
the original event. This keeps the concrete implementations ([mem](../mem), [jpa](../jpa), [grpc](../esgrpc)) completely
unaware that encryption is applied.

The actual cryptography is **not** part of this module. It relies on the algorithm-agnostic
[EncryptedDataService](https://github.com/fuinorg/objects4j/blob/master/crypto/src/main/java/org/fuin/objects4j/crypto/EncryptedDataService.java)
contract from [objects4j-crypto](https://github.com/fuinorg/objects4j/tree/master/crypto), which you implement with the
cipher and key store of your choice.

## Types

| Type                                                                                  | Description                                                                                                  |
|:--------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| [EncryptingEventStore](src/main/java/org/fuin/esc/crypto/EncryptingEventStore.java)   | Event store decorator that encrypts on write and decrypts on read. Created via its `Builder`.                |
| [KeyIdResolver](src/main/java/org/fuin/esc/crypto/KeyIdResolver.java)                 | Selects the key identifier for an event. An empty `Optional` means the event is stored unencrypted.          |
| [FixedKeyIdResolver](src/main/java/org/fuin/esc/crypto/FixedKeyIdResolver.java)       | Trivial resolver that uses the same key for every event. Mainly used for tests.                              |
| [EncryptedDataFactory](src/main/java/org/fuin/esc/crypto/EncryptedDataFactory.java)   | Creates the serializable `EncryptedData` representation for the binding in use (JSON-B, JAXB, Jackson).      |
| [EscEncryptionException](src/main/java/org/fuin/esc/crypto/EscEncryptionException.java)| Unchecked wrapper for the checked encryption exceptions so the `EventStore` method signatures are preserved. |

The serializable `EscEncryptedData` representations and matching `EncryptedDataFactory` implementations live in the
serialization modules: [esc-jsonb](../jsonb), [esc-jaxb](../jaxb) and [esc-jackson](../jackson).

## What gets encrypted

By default only the event **data** is encrypted, while the **metadata** stays in plain text so it remains usable for
routing and projections. Use `Builder.encryptMeta(true)` to also encrypt the metadata.

The data type name is hidden at rest: an encrypted event is stored under the `EscEncryptedData` type, and the original
type and content type are kept inside the `EncryptedData` so the event can be reconstructed on read.

## Usage

```java
// Your own implementation of the objects4j-crypto contract
final EncryptedDataService encryptionService = ...;

final EventStore delegate = ...; // e.g. InMemoryEventStore, JpaEventStore, ...

final EventStore es = new EncryptingEventStore.Builder()
        .delegate(delegate)
        .serRegistry(serializerRegistry)        // serializes the event data before encryption
        .desRegistry(deserializerRegistry)      // deserializes after decryption
        .encryptionService(encryptionService)
        .keyIdResolver(new FixedKeyIdResolver("my-key"))
        .encryptedDataFactory(new EscEncryptedDataFactory()) // from esc-jsonb / esc-jaxb / esc-jackson
        // .encryptMeta(true)                   // optional, default false
        // .failOnUndecryptable(false)          // optional, default true
        .build();

es.open();
es.appendToStream(streamId, event);   // data is encrypted before it reaches the delegate
final CommonEvent read = es.readEvent(streamId, 0); // transparently decrypted
```

Make sure the `EscEncryptedData` type of the chosen binding is registered in the serializer/deserializer registries of
the delegate store (the `addEscTypes` / `addEscSerDeserializer` helpers in `EscJsonbUtils` / `EscJaxbUtils` /
`EscJacksonUtils` already include it).

## Behaviour notes

- **Selective / mixed streams** &ndash; if the `KeyIdResolver` returns an empty `Optional` the event is stored in plain
  text, so plaintext and encrypted events can coexist in the same stream.
- **Idempotency** &ndash; events whose data is already an `EncryptedData` are passed through unchanged (no double
  encryption).
- **Key rotation** &ndash; decryption uses the key version stored inside the `EncryptedData`, so historical events keep
  decrypting correctly after a key has been rotated.
- **Missing key** &ndash; by default reading an event that cannot be decrypted throws an
  [EscEncryptionException](src/main/java/org/fuin/esc/crypto/EscEncryptionException.java); set
  `failOnUndecryptable(false)` to instead return the event with the encrypted wrapper still in place.
