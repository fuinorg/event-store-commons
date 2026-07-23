# Resilience Tasks — event-store-commons (esc)

What is implemented, and what was deliberately left out, is described in [resilience.md](resilience.md).
Only open points are listed here.

**Nothing is scheduled.** The active work is in
[cqrs-4-java](https://github.com/fuinorg/cqrs-4-java/blob/develop/resilience-tasks.md).

---

## Next

Nothing.

---

## Nice to have

- **A configuration-property surface for the call timeouts.** They are set through the builder or the
  constructor only, so an operator cannot retune them without a code change. Revisit when `cqrs-4-java`'s
  Quarkus and Spring modules next map their configuration onto these builders — that is the point at which it
  becomes clear whether a property is needed here at all, or whether the application layer is the better
  place for it. A `System.getProperty` fallback consulted by the builders would keep these modules
  framework-free.
