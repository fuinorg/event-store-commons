// CHECKSTYLE:OFF
package org.fuin.esc.esjc;

import com.github.msemys.esjc.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class NativeDeleteExampleApp {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        EventStore es = EventStoreBuilder.newBuilder().singleNodeAddress("127.0.0.1", 1113).build();
        es.connect();
        try {

            // Create stream by appending something to it
            String streamId = "MyStream";
            List<EventData> events = new ArrayList<>();
            events.add(EventData.newBuilder().eventId(UUID.randomUUID()).type("baz").data("dummy content")
                    .build());
            es.appendToStream(streamId, ExpectedVersion.ANY, events).join();

            // Hard delete
            es.deleteStream(streamId, ExpectedVersion.ANY, true).join();

            // Get status
            final StreamMetadataResult result = es.getStreamMetadata(streamId).get();
            if (result.isStreamDeleted) {
                // === THIS IS PRINTED ===
                System.out.println("SOFT DELETED");
            } else {
                System.out.println("HARD DELETED");
            }

        } finally {
            es.disconnect();
        }

    }

}
// CHECKSTYLE:ON
