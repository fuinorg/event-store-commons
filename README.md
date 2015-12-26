# event-store-commons
Defines a common event store Java interface and provides some adapters (like for Greg Young's [event store](https://www.geteventstore.com/)) and implementations (like in-memory or file-based).

# Status
**This is work in progress** - *Don't expect the interfaces to be stable or the code to be well tested yet...*

| Module | Description | Status | Comment |
|--------|-------------|--------|---------|
| [esc-api](api) | Defines the event store commons API. | OK | Test coverage ~48% |
| [esc-http](eshttp) | HTTP adapter for Greg Young's [event store](https://www.geteventstore.com/)| OK | Test coverage ~55% |
| [esc-esj](esj) | Adapter for [lt.emasina:esj-client](https://github.com/valdasraps/esj) | Compile Errors  | Will be dropped soon because 'esj' is incomplete |
| [esc-jpa](jpa) | JPA adapter | Development paused | Will be continued after 'esc-test' is more complete |
| [esc-mem](mem) | In-memory implementation | OK | Test coverage ~45% |
| [esc-spi](spi) | Helper classes for adapters and implementations | OK | Test coverage ~65% |
| [esc-test](test) | Cucumber tests for adapters and implementations | OK |  |

# Architecture
![Layers](https://raw.github.com/fuinorg/event-store-commons/master/doc/event-store-commons.png)
                                      