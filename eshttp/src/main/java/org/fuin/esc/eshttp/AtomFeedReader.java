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

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.DeserializerRegistry;

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
     * @param in
     *            Input stream to read.
     * 
     * @return Event.
     */
    public CommonEvent readEvent(DeserializerRegistry desRegistry, InputStream in);

}
