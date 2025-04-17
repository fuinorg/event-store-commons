# esc-mem

In-memory implementation of the event store commons api for unit testing.

**LIMITATIONS**: This implementation can only handle integer values (not long) internally. This means event numbers
larger that the max integer value will lead to an exception. 