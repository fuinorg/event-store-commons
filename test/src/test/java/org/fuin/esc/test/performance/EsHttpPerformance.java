package org.fuin.esc.test.performance;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Performance test for the ESC HTTP Event Store (https://geteventstore.com/) implementation.
 */
public final class EsHttpPerformance {

    private EsHttpPerformance() {
        super();
    }

    /**
     * Main method.
     *
     * @param args Not used.
     * @throws IOException Error reading console input.
     */
    public static void main(final String[] args) throws IOException {
/*
        
        waitForInput("START");

        final int max = 100000;

        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        final URL url = new URL("http://127.0.0.1:2113/");

        final SerializedDataType serMetaType = new SerializedDataType("MyMeta");
        final SerializedDataType serDataType = new SerializedDataType("BookAddedEvent");

        final JsonDeSerializer jsonDeSer = new JsonDeSerializer();

        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(serDataType, "application/json", jsonDeSer);
        registry.add(serMetaType, "application/json", jsonDeSer);

        final EventStore eventStore = new ESHttpEventStore(threadFactory, url, ESEnvelopeType.JSON,
                registry, registry);
        eventStore.open();
        try {

            final TypeName dataType = new TypeName("BookAddedEvent");
            final TypeName metaType = new TypeName("MyMeta");
            final JsonObject event = Json.createObjectBuilder().add("name", "Shining")
                    .add("author", "Stephen King").build();
            final JsonObject meta = Json.createObjectBuilder().add("user", "michael").build();

            waitForInput("APPEND 1");

            // Append all together
            final StreamId streamId1 = new SimpleStreamId("books1");
            final List<CommonEvent> togetherList = new ArrayList<>();
            for (int i = 0; i < max; i++) {
                togetherList.add(new SimpleCommonEvent(new EventId(), dataType, event, metaType, meta));
            }
            measure("One append (" + togetherList.size() + " events)", togetherList.size(),
                    none -> eventStore.appendToStream(streamId1, ExpectedVersion.ANY.getNo(), togetherList));

            waitForInput("APPEND 2");

            // Append all separate
            final StreamId streamId2 = new SimpleStreamId("books2");
            final List<CommonEvent> separateList = new ArrayList<>();
            for (int i = 0; i < max; i++) {
                separateList.add(new SimpleCommonEvent(new EventId(), dataType, event, metaType, meta));
            }
            measure("Multiple appends (" + separateList.size() + " events)", separateList.size(), none -> {
                for (final CommonEvent ce : separateList) {
                    eventStore.appendToStream(streamId2, ExpectedVersion.ANY.getNo(), ce);
                }
            });

            waitForInput("READ 1");

            // Read big chunks
            final int chunkSize = 4000;
            measure("Read chunks of " + chunkSize + " (" + max + " events)", max, name -> {
                int start = 0;
                int count = 0;
                StreamEventsSlice slice;
                do {
                    slice = eventStore.readEventsForward(streamId1, start, chunkSize);
                    start = slice.getNextEventNumber();
                    count = count + slice.getEvents().size();
                } while (!slice.isEndOfStream());
                System.out.println("NO OF EVENTS   " + name + ": " + count);
            });

            waitForInput("READ 2");

            // Read single
            measure("Read one by one (" + max + " events)", max, name -> {
                int count = 0;
                int start = 0;
                StreamEventsSlice slice;
                do {
                    slice = eventStore.readEventsForward(streamId2, start, 1);
                    start = slice.getNextEventNumber();
                    count = count + slice.getEvents().size();
                    if (count % 1000 == 0) {
                        System.out.print(".");
                    }
                } while (!slice.isEndOfStream());
                System.out.println();
                System.out.println("NO OF EVENTS   " + name + ": " + count);
            });

        } finally {
            eventStore.close();
        }

        waitForInput("END");
*/
    }

    private static void measure(final String name, final int count, final Consumer<String> func) {
        final long start = System.currentTimeMillis();
        System.out.println("START          " + name + ": " + start);
        func.accept(name);
        final long end = System.currentTimeMillis();
        System.out.println("END            " + name + ": " + end);
        final double millis = end - start;
        System.out.println("DIFF           " + name + ": " + (long) millis);
        final double seconds = millis / 1000.0;
        System.out.println("SEC            " + name + ": " + (long) seconds);
        final double perSec = (count / millis) * 1000.0;
        System.out.println("EVENTS PER SEC " + name + ": " + (long) perSec);
    }

    private static void waitForInput(final String message) throws IOException {
        System.out.println("---------------------");
        System.out.println(message);
        System.out.println("Press 'c'<ENTER> to continue");
        while ((char) System.in.read() != 'c') {
            // Do it again
        }

    }

}

