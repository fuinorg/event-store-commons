/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.eshttp;

import java.nio.charset.Charset;

/**
 * Type of data sent to and received from the event store.
 */
public enum ESEnvelopeType {

    /** XML envelope. */
    XML("application/xml", "utf-8", "application/atom+xml", "application/vnd.eventstore.events+xml",
            new ESHttpMarshaller(), new AtomFeedXmlReader()),

    /** JSON enevlope. */
    JSON("application/json", "utf-8", "application/vnd.eventstore.atom+json",
            "application/vnd.eventstore.events+json", new ESHttpMarshaller(), new AtomFeedJsonReader());

    private final String metaType;

    private final Charset metaCharset;

    private final String readContentType;

    private final String writeContentType;

    private final ESHttpMarshaller marshaller;

    private final AtomFeedReader atomFeedReader;

    private ESEnvelopeType(final String metaType, final String metaCharset, final String readContentType,
            final String writeContentType, final ESHttpMarshaller marshaller,
            final AtomFeedReader atomFeedReader) {
        this.metaType = metaType;
        this.metaCharset = Charset.forName(metaCharset);
        this.readContentType = readContentType;
        this.writeContentType = writeContentType;
        this.marshaller = marshaller;
        this.atomFeedReader = atomFeedReader;
    }

    /**
     * Mime type used for meta data.
     * 
     * @return Mime type.
     */
    public final String getMetaType() {
        return metaType;
    }

    /**
     * Charset used for meta data.
     * 
     * @return Charset.
     */
    public final Charset getMetaCharset() {
        return metaCharset;
    }

    /**
     * Content type used for reading.
     * 
     * @return Content type for reading.
     */
    public final String getReadContentType() {
        return readContentType;
    }

    /**
     * Content type used for writing.
     * 
     * @return Content type for writing.
     */
    public final String getWriteContentType() {
        return writeContentType;
    }

    /**
     * Returns the marshaller to use.
     * 
     * @return Marshaller.
     */
    public final ESHttpMarshaller getMarshaller() {
        return marshaller;
    }

    /**
     * Returns the reader to use.
     * 
     * @return Reads Atom feeds.
     */
    public final AtomFeedReader getAtomFeedReader() {
        return atomFeedReader;
    }

}
