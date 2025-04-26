package org.fuin.esc.esgrpc.example;

import io.kurrent.dbclient.DeleteResult;
import io.kurrent.dbclient.DeleteStreamOptions;
import io.kurrent.dbclient.EventData;
import io.kurrent.dbclient.KurrentDBClient;
import io.kurrent.dbclient.KurrentDBClientSettings;
import io.kurrent.dbclient.KurrentDBConnectionString;
import io.kurrent.dbclient.ReadResult;
import io.kurrent.dbclient.ReadStreamOptions;
import io.kurrent.dbclient.ResolvedEvent;
import io.kurrent.dbclient.WriteResult;
import org.fuin.utils4j.TestOmitted;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

@TestOmitted("Example class")
@SuppressWarnings("java:S106")
public class App {

    private App() {
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        System.out.println("BEGIN");

        KurrentDBClientSettings setts = KurrentDBConnectionString.parseOrThrow("esdb://localhost:2113?tls=false");
        KurrentDBClient client = KurrentDBClient.create(setts);

        String json = """
                { 
                    "id" : "3b92d23e-837a-441e-b59e-1733182f741c",
                    "login" : "ouros" 
                }
                """;

        EventData event = EventData.builderAsJson("account-created", json.getBytes(StandardCharsets.UTF_8)).build();

        String streamName = "accounts3";

        WriteResult writeResult = client.appendToStream(streamName, event).get();
        System.out.println("writeResult=" + writeResult);

        ReadStreamOptions readStreamOptions = ReadStreamOptions.get().fromStart().maxCount(1).notResolveLinkTos();

        ReadResult readResult = client.readStream(streamName, readStreamOptions).get();
        System.out.println("readResult=" + readResult);

        ResolvedEvent resolvedEvent = readResult.getEvents().get(0);
        System.out.println("resolvedEvent=" + resolvedEvent);

        String writtenEvent = new String(resolvedEvent.getOriginalEvent().getEventData(), StandardCharsets.UTF_8);
        System.out.println("writtenEvent=" + writtenEvent);

        DeleteResult deleteResult = client.tombstoneStream(streamName, DeleteStreamOptions.get()).get();
        System.out.println("deleteResult=" + deleteResult);

        System.out.println("END");

    }

}