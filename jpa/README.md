# esc-jpa
Java Persistence API (JPA) based implementation of the event store commons api.

Entity types
------------
There are two different scenarios: 

1. Streams that require one or more discriminator columns 
2. Streams without the need for a discriminator column

An example for streams with discriminator columns is the below mentioned "Vendor" aggregate. There is only one
stream entity (table) and one event entity (table) for **all** vendors. The vendor's ID is used as discriminator
column to distinguish events for the different vendors.

The discriminator is coded in the [stream identifier](https://github.com/fuinorg/event-store-commons/blob/master/api/src/main/java/org/fuin/esc/api/StreamId.java). An example of an identifier with discriminator column is 
the [AggregateStreamId](src/test/java/org/fuin/esc/jpa/examples/AggregateStreamId.java) and another one for an
identifier without a discriminator is the [SimpleStreamId](https://github.com/fuinorg/event-store-commons/blob/master/api/src/main/java/org/fuin/esc/api/SimpleStreamId.java).

The good new is: There is no need to create any entities if you don't need a discriminator column. There are already
two predefined entites [NoParamsEvent](src/main/java/org/fuin/esc/jpa/NoParamsEvent.java) and
[NoParamsStream](src/main/java/org/fuin/esc/jpa/NoParamsStream.java) that will be used automatically in this case.
For streams that require discriminator columns you have to create two entity classes ("XyzEvent" + "XyzStream") named as described below.

Custom naming of tables and entities
------------------------------------
You can use [JpaStreamId](src/main/java/org/fuin/esc/jpa/JpaStreamId.java) to define the names of your stream entities and classes.
There are two predfined classes [SimpleJpaStreamId](src/main/java/org/fuin/esc/jpa/SimpleJpaStreamId.java) and
[ProjectionJpaStreamId](src/main/java/org/fuin/esc/jpa/ProjectionJpaStreamId.java) that already implement this interface.


Default naming conventions
--------------------------
If you don't define an explicit stream name using a [JpaStreamId](src/main/java/org/fuin/esc/jpa/JpaStreamId.java),
a strict default naming convention applies. **You have to name your tables and identifiers exactly as described below otherwise it will not work**.

####StreamId
Use a camel case stream identifier name that ends *not* on 'Stream', 'Streams', 'Event' or 'Events'.
```java
StreamId streamId = new AggregateStreamId("YourSelectedName", discriminatorValue);
```

####Event entity
Table name is the same as the stream identifier plus '_EVENTS' and uses an 'underscore' for replacing camel case parts.
Class name is the same as the stream identifier plus 'Event' (Note that there is no 's' at the end!).
```java
@Table(name = "YOUR_SELECTED_NAME_EVENTS")
@Entity
public class YourSelectedNameEvent extends JpaStreamEvent { 
    // ... 
}
```

####Stream entity 
Table name is the same as the stream identifier plus '_STREAMS' and uses an 'underscore' for replacing camel case parts.   
Class name is the same as the stream identifier plus 'Stream' (Note that there is no 's' at the end!).
```java
@Table(name = "APPEND_MULTIPLE_AGAIN_STREAMS")
@Entity
public class YourSelectedNameStream extends JpaStream { 
    // ... 
}
```

Example JPA entity structure
----------------------------
The UML diagram shows how events of an example "Vendor" aggregate are stored in the database. 
A [VendorStream](src/test/java/org/fuin/esc/jpa/examples/VendorStream.java) has information about the stream itself 
and [VendorEvent](src/test/java/org/fuin/esc/jpa/examples/VendorEvent.java) has a reference to the event table. 
The [JpaEvent](src/main/java/org/fuin/esc/jpa/JpaEvent.java) table contains the real event data.    
![JPA Entities](https://raw.github.com/fuinorg/event-store-commons/master/jpa/src/main/doc/esc-jpa-example.png)

