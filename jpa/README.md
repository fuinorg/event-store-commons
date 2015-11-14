# esc-jpa
Java Persistence API (JPA) based implementation of the event store commons api.

Example JPA entity structure
----------------------------
The UML diagram shows how events of an example "Vendor" aggregate are stored in the database. 
A [VendorStream](src/test/java/org/fuin/esc/jpa/examples/VendorStream.java) has information about the stream itself 
and [VendorEvent](src/test/java/org/fuin/esc/jpa/examples/VendorEvent.java) has a reference to the event table. 
The [JpaEvent](src/main/java/org/fuin/esc/jpa/JpaEvent.java) table contains the real event data.    
![JPA Entities](https://raw.github.com/fuinorg/event-store-commons/master/jpa/src/main/doc/esc-jpa-example.png)
