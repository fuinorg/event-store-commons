# event-store-commons
Defines a common event store Java interface and provides some adapters (like for Greg Young's [event store](https://www.geteventstore.com/)) and implementations (like in-memory or file-based).

# Status
![Warning](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/warning.gif) **This is work in progress** - *Don't expect the interfaces to be stable or the code to be well tested yet...* ![Warning](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/warning.gif)

| Module | Description | Status | Comment |
|--------|-------------|--------|---------|
| [esc-api](api) | Defines the event store commons API. | ![OK](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/ok.png) | Test coverage ~58% |
| [esc-http](eshttp) | HTTP adapter for Greg Young's [event store](https://www.geteventstore.com/)| ![OK](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/ok.png) | Test coverage ~66% |
| [esc-jpa](jpa) | JPA adapter | ![PAUSED](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/sleeping.png) | Development paused - Will be continued after 'esc-test' is more complete |
| [esc-mem](mem) | In-memory implementation | ![OK](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/ok.png) | Test coverage ~60% |
| [esc-spi](spi) | Helper classes for adapters and implementations | ![OK](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/ok.png) | Test coverage ~68% |
| [esc-test](test) | Cucumber tests for adapters and implementations | ![Work in progress](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/work-in-progress.png) | Currently in work |

# Architecture
![Layers](https://raw.github.com/fuinorg/event-store-commons/master/doc/event-store-commons.png)
See [ES.JVM](https://github.com/EventStore/EventStore.JVM) / [ES.Java](https://github.com/jen20/EventStore.Java)