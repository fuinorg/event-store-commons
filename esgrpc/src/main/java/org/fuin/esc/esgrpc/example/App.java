package org.fuin.esc.esgrpc.example;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.eventstore.dbclient.DeleteResult;
import com.eventstore.dbclient.DeleteStreamOptions;
import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.WriteResult;

public class App {

    public static void main(String args[]) throws InterruptedException, ExecutionException, IOException {

        System.out.println("BEGIN");

        EventStoreDBClientSettings setts = EventStoreDBConnectionString.parseOrThrow("esdb://localhost:2113?tls=false");
        EventStoreDBClient client = EventStoreDBClient.create(setts);

        AccountCreated createdEvent = new AccountCreated();

        createdEvent.setId(UUID.randomUUID());
        createdEvent.setLogin("ouros");

        EventData event = EventData.builderAsJson("account-created", createdEvent).build();

        String streamName = "accounts3";

        WriteResult writeResult = client.appendToStream(streamName, event).get();
        System.out.println("writeResult=" + writeResult);

        ReadStreamOptions readStreamOptions = ReadStreamOptions.get().fromStart().maxCount(1).notResolveLinkTos();

        ReadResult readResult = client.readStream(streamName, readStreamOptions).get();
        System.out.println("readResult=" + readResult);

        ResolvedEvent resolvedEvent = readResult.getEvents().get(0);
        System.out.println("resolvedEvent=" + resolvedEvent);

        AccountCreated writtenEvent = resolvedEvent.getOriginalEvent().getEventDataAs(AccountCreated.class);
        System.out.println("writtenEvent=" + writtenEvent);

        DeleteResult deleteResult = client.tombstoneStream(streamName, DeleteStreamOptions.get()).get();
        System.out.println("deleteResult=" + deleteResult);


        System.out.println("END");

    }
}