# event-store-commons

Defines a common event store Java interface and provides some adapters (like for Greg
Young's [event store](https://www.geteventstore.com/)) and implementations (like in-memory or file-based).

[![Java Maven Build](https://github.com/fuinorg/event-store-commons/actions/workflows/maven.yml/badge.svg)](https://github.com/fuinorg/event-store-commons/actions/workflows/maven.yml)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.fuin.esc%3Aevent-store-commons&metric=coverage)](https://sonarcloud.io/dashboard?id=org.fuin.esc%3Aevent-store-commons)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.fuin.esc/esc-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.fuin.esc/esc-parent/)
[![LGPLv3 License](http://img.shields.io/badge/license-LGPLv3-blue.svg)](https://www.gnu.org/licenses/lgpl.html)
[![Java Development Kit 17](https://img.shields.io/badge/JDK-17-green.svg)](https://openjdk.java.net/projects/jdk/17/)

## Versions
- [0.9.0](release-notes.md#090)
- [0.8.0](release-notes.md#080)
- 0.7.x = New **GRPC** client / Removed **http**/**esjc** modules
- 0.6.x = **Java 17** and JUnit5
- 0.5.x = **Java 11** with new **jakarta** namespace
- 0.4.x = **Java 11** before namespace change from 'javax' to 'jakarta'
- 0.3.2 (or less) = **Java 8**

## Status

![Warning](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/warning.gif) **This is work in progress** ![Warning](https://raw.githubusercontent.com/fuinorg/event-store-commons/master/doc/warning.gif)

| Module               | Description                                                                                                                                                      |
|:---------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [esc-api](api)       | Defines the event store commons API.                                                                                                                             |
| [esc-grpc](grpc)     | [Event Store DB Client](https://github.com/EventStore/KurrentDB-Client-Java) based implementation for Greg Young's [event store](https://www.geteventstore.com/) |
| [esc-jacoco](jacoco) | Helper module to collect JaCoco results                                                                                                                          |
| [esc-jaxb](jaxb)     | JAX-B serialization support                                                                                                                                      |
| [esc-jpa](jpa)       | JPA based implementation (events are stored in a relational database)                                                                                            |
| [esc-jsonb](jsonb)   | JSON-B serialization support                                                                                                                                     |
| [esc-mem](mem)       | In-memory implementation (events are not persisted)                                                                                                              |
| [esc-spi](spi)       | Helper classes for adapters and implementations                                                                                                                  |
| [esc-test](test)     | Cucumber tests for adapters and implementations                                                                                                                  |

## Architecture

![Layers](https://raw.github.com/fuinorg/event-store-commons/master/doc/event-store-commons.png)

## Examples

- [Simple in-memory example](test/src/test/java/org/fuin/esc/test/examples/InMemoryExample.java)
- [Event store with GRPC interface and JSON (JSON-B)](test/src/test/java/org/fuin/esc/test/examples/EsGrpcJsonbExample.java)

### Snapshots

Snapshots can be found on
the [OSS Sonatype Snapshots Repository](https://oss.sonatype.org/content/repositories/snapshots/org/fuin/esc/ "Snapshot Repository").

Add the following to
your [.m2/settings.xml](http://maven.apache.org/ref/3.2.1/maven-settings/settings.html "Reference configuration") to
enable snapshots in your Maven build:

```xml
<repository>
    <id>sonatype.oss.snapshots</id>
    <name>Sonatype OSS Snapshot Repository</name>
    <url>http://oss.sonatype.org/content/repositories/snapshots</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```
