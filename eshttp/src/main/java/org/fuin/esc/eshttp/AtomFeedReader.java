package org.fuin.esc.eshttp;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.SerializedDataType;

/**
 * Reads and an Atom feed.
 */
public interface AtomFeedReader {

    /**
     * Parses the URIs of the events from the ATOM feed.
     * 
     * @param in
     *            Input stream to read.
     * 
     * @return List of event URIs in the order they appeared in the feed.
     */
    public List<URI> readAtomFeed(InputStream in);

    /**
     * Reads an event.
     * 
     * @param desRegistry
     *            Registry with known deserializers.
     * @param serMetaType
     *            Unique name of the meta data type.
     * @param in
     *            Input stream to read.
     * 
     * @return Event.
     */
    public CommonEvent readEvent(DeserializerRegistry desRegistry, SerializedDataType serMetaType, InputStream in);


}
