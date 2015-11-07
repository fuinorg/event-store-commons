# esc-jpa
Java Persistence API (JPA) based implementation of the event store commons api.

Example JPA entity structure
----------------------------
The UML diagram shows how events of an example "Vendor" aggregate are stored in the database. 
A [VendorStream](src/test/java/org/fuin/esc/jpa/examples/VendorStream.java) has information about the stream itself 
and [VendorEvent](src/test/java/org/fuin/esc/jpa/examples/VendorEvent.java) has the events.   
![JPA Entities](https://raw.github.com/fuinorg/event-store-commons/master/jpa/src/main/doc/esc-jpa-example.png)
