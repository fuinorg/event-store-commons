package org.fuin.esc.esj;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.json.Json;

import lt.emasina.esj.EventStore;
import lt.emasina.esj.ResponseReceiver;
import lt.emasina.esj.Settings;
import lt.emasina.esj.model.Event;
import lt.emasina.esj.model.Message;
import lt.emasina.esj.model.UserCredentials;
import lt.emasina.esj.model.converter.ByteArrayToByteStringConverter;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ProjectionNotWritableException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.esc.api.WritableEventStore;
import org.fuin.objects4j.common.Contract;

public class EventStoreESJ implements WritableEventStore {

    private final InetAddress host;

    private final int port;

    private final Settings settings;

    private final ExecutorService executor;

    private final String user;

    private final String password;

    private EventStore es;

    public EventStoreESJ(final InetAddress host, final int port,
            final Settings settings, final ExecutorService executor,
            final String user, final String password) {
        super();
        this.host = host;
        this.port = port;
        this.settings = settings;
        this.executor = executor;
        this.user = user;
        this.password = password;
    }

    @Override
    public void open() {
        try {
            es = new EventStore(host, port, settings, executor,
                    new UserCredentials(user, password));
        } catch (final IOException ex) {
            throw new RuntimeException("Error opening event store", ex);
        }
    }

    @Override
    public void close() {
        try {
            es.close();
        } catch (final Exception ex) {
            throw new RuntimeException("Error closing event store", ex);
        }
    }

    @Override
    public CommonEvent readEvent(StreamId streamId, int eventNumber)
            throws EventNotFoundException, StreamNotFoundException,
            StreamDeletedException {

        es.readFromStream(streamId.asString(), eventNumber,
                new ResponseReceiver() {
                    @Override
                    public void onResponseReturn(final Message msg) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onErrorReturn(final Exception ex) {
                        // TODO Auto-generated method stub
                    }
                });

        return null;
    }

    @Override
    public StreamEventsSlice readStreamEventsForward(StreamId streamId,
            int start, int count) throws StreamNotFoundException,
            StreamDeletedException {

        es.readAllEventsForward(streamId.asString(), start, count,
                new ResponseReceiver() {
                    @Override
                    public void onResponseReturn(final Message msg) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onErrorReturn(final Exception ex) {
                        // TODO Auto-generated method stub
                    }
                });

        return null;
    }

    @Override
    public StreamEventsSlice readAllEventsForward(int start, int count) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteStream(StreamId streamId, int expectedVersion)
            throws StreamNotFoundException, StreamVersionConflictException,
            StreamDeletedException {

        es.deleteStream(streamId.asString(), new ResponseReceiver() {
            @Override
            public void onResponseReturn(final Message msg) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onErrorReturn(final Exception ex) {
                // TODO Auto-generated method stub
            }
        });

    }

    @Override
    public void deleteStream(StreamId streamId) throws StreamNotFoundException,
            StreamDeletedException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public int appendToStream(final StreamId streamId,
            final int expectedVersion, final CommonEvent... events)
            throws StreamNotFoundException, StreamVersionConflictException,
            StreamDeletedException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("events", events);

        return appendToStream(streamId, expectedVersion, Arrays.asList(events));

    }

    @Override
    public int appendToStream(StreamId streamId, int expectedVersion,
            List<CommonEvent> commonEvents) throws StreamNotFoundException,
            StreamVersionConflictException, StreamDeletedException,
            ProjectionNotWritableException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("eventList", commonEvents);

        final List<Event> esjEvents = new ArrayList<Event>();
        for (final CommonEvent commonEvent : commonEvents) {

            final UUID id = UUID.fromString(commonEvent.getId());
            final String type = commonEvent.getType();
            final Object data = commonEvent.getData();
            Object meta = commonEvent.getMeta();
            if (meta == null) {
                meta = Json.createObjectBuilder().add("content-type", contentType);
            }

            // Prepare converter
            final ByteArrayToByteStringConverter dataConv = new ByteArrayToByteStringConverter(
                    data.isJson());
            final ByteArrayToByteStringConverter metaConv = new ByteArrayToByteStringConverter(
                    meta.isJson());

            esjEvents.add(new Event(id, type, data, dataConv, meta, metaConv));

        }

        es.appendToStream(streamId.asString(), expectedVersion,
                new ResponseReceiver() {
                    @Override
                    public void onResponseReturn(final Message msg) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onErrorReturn(final Exception ex) {
                        // TODO Auto-generated method stub
                    }
                }, esjEvents);

        return 0;
    }

    @Override
    public int appendToStream(StreamId streamId, List<CommonEvent> events)
            throws StreamNotFoundException, StreamDeletedException,
            ProjectionNotWritableException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int appendToStream(StreamId streamId, CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException {
        // TODO Auto-generated method stub
        return 0;
    }

}
