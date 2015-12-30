# event-store-commons
Defines a common event store Java interface and provides some adapters (like for Greg Young's [event store](https://www.geteventstore.com/)) and implementations (like in-memory or file-based).

[![Build Status](https://fuin-org.ci.cloudbees.com/job/event-store-commons/badge/icon)](https://fuin-org.ci.cloudbees.com/job/event-store-commons/)
[![Coverage Status](https://coveralls.io/repos/fuinorg/event-store-commons/badge.svg)](https://coveralls.io/r/fuinorg/event-store-commons)
[![LGPLv3 License](http://img.shields.io/badge/license-LGPLv3-blue.svg)](https://www.gnu.org/licenses/lgpl.html)
[![Java Development Kit 1.8](https://img.shields.io/badge/JDK-1.8-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

*Caution*: The code coverage value is not correct (it's actually higher than above value) as the 'test' module is not considered correctly (See [Issue #4](https://github.com//fuinorg/event-store-commons/issues/4))


## Status
![Warning](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/warning.gif) **This is work in progress** ![Warning](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/warning.gif)

| Module | Description | Status | Comment |
|:-------|:------------|--------|:--------|
| [esc-api](api) | Defines the event store commons API. | ![OK](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/ok.png) | Test coverage ~58% |
| [esc-http](eshttp) | HTTP adapter for Greg Young's [event store](https://www.geteventstore.com/)| ![OK](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/ok.png) | Test coverage ~66% |
| [esc-jpa](jpa) | JPA adapter | ![PAUSED](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/sleeping.png) | Development paused - Will be continued after 'esc-test' is more complete |
| [esc-mem](mem) | In-memory implementation | ![OK](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/ok.png) | Test coverage ~60% |
| [esc-spi](spi) | Helper classes for adapters and implementations | ![OK](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/ok.png) | Test coverage ~68% |
| [esc-test](test) | Cucumber tests for adapters and implementations | ![Work in progress](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/work-in-progress.png) | Currently in work |

## Architecture
![Layers](https://raw.github.com/fuinorg/event-store-commons/master/doc/event-store-commons.png)
See [ES.JVM](https://github.com/EventStore/EventStore.JVM) / [ES.Java](https://github.com/jen20/EventStore.Java)

## Examples
- [Simple in-memory example](test/src/test/java/org/fuin/esc/test/examples/InMemoryExample.java)
- [Event store with HTTP interface and XML](test/src/test/java/org/fuin/esc/test/examples/EsHttpXmlExample.java)
- [Event store with HTTP interface and JSON](test/src/test/java/org/fuin/esc/test/examples/EsHttpJsonExample.java)
- [Event store with HTTP interface and mixed JSON/XML content](test/src/test/java/org/fuin/esc/test/examples/EsHttpMixedExample.java)
