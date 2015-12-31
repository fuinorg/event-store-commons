# esc-jpa
Java Persistence API (JPA) based implementation of the event store commons api.

Naming conventions
------------------
The JPA implementation requires a strict naming convention. You have to name your tables and identifiers accordingly.

####StreamId
Use a camel case stream identifier name that ends *not* on 'Stream', 'Streams', 'Event' or 'Events'.
```java
StreamId streamId = new SimpleStreamId("YourSelectedName")
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

