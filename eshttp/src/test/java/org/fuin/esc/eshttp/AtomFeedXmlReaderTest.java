/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.eshttp;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.fuin.esc.spi.EnhancedMimeType;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Tests the {@link AtomFeedXmlReader} class.
 */
// CHECKSTYLE:OFF Test
public class AtomFeedXmlReaderTest {

    @Test
    public void testReadAtomFeed() throws IOException, URISyntaxException {

        // PREPARE
        final AtomFeedXmlReader testee = new AtomFeedXmlReader();
        final InputStream in = this.getClass().getResourceAsStream("/atom-feed-stream.xml");
        try {

            // TEST
            final List<URI> uris = testee.readAtomFeed(in);

            // VERIFY
            assertThat(uris).containsExactly(new URI("classpath://atom-feed-event-1.xml"),
                    new URI("classpath://atom-feed-event-0.xml"));

        } finally {
            in.close();
        }

    }

    @Test
    public void testReadAtomEntryContent() throws IOException, SAXException {

        // PREPARE
        final AtomFeedXmlReader testee = new AtomFeedXmlReader();
        final InputStream in = this.getClass().getResourceAsStream("/atom-feed-event-0.xml");
        try {

            // TEST
            final AtomEntry<?> entry = testee.readAtomEntry(in);

            // VERIFY
            assertThat(entry.getEventStreamId()).isEqualTo("MyStreamA");
            assertThat(entry.getEventNumber()).isEqualTo(0);
            assertThat(entry.getEventType()).isEqualTo("MyEvent");
            assertThat(entry.getEventId()).isEqualTo("8faef866-b80f-4952-9124-62819a6517aa");
            assertThat(entry.getDataContentType()).isEqualTo(
                    EnhancedMimeType.create("application/xml; encoding=UTF-8"));
            assertThat(entry.getMetaContentType()).isEqualTo(
                    EnhancedMimeType.create("application/xml; encoding=UTF-8"));
            assertThat(entry.getData()).isInstanceOf(Node.class);
            assertThat(entry.getMeta()).isInstanceOf(Node.class);

        } finally {
            in.close();
        }

    }

}
// CHECKSTYLE:ON
