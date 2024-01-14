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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import jakarta.json.JsonObject;

import org.fuin.esc.api.EnhancedMimeType;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link AtomFeedJsonReader} class.
 */
// CHECKSTYLE:OFF Test
public class AtomFeedJsonReaderTest {

    @Test
    public void testReadAtomFeed() throws IOException, URISyntaxException {

        // PREPARE
        final AtomFeedJsonReader testee = new AtomFeedJsonReader();
        final InputStream in = this.getClass().getResourceAsStream("/atom-feed-stream.json");
        try {

            // TEST
            final List<URI> uris = testee.readAtomFeed(in);

            // VERIFY
            assertThat(uris).containsExactly(new URI("classpath://atom-feed-event-1.json"),
                    new URI("classpath://atom-feed-event-0.json"));

        } finally {
            in.close();
        }

    }

    @Test
    public void testReadAtomEntryContent() throws IOException {

        // PREPARE
        final AtomFeedJsonReader testee = new AtomFeedJsonReader();
        final InputStream in = this.getClass().getResourceAsStream("/atom-feed-event-0.json");
        try {

            // TEST
            final AtomEntry<?> entry = testee.readAtomEntry(in);

            // VERIFY
            assertThat(entry.getEventStreamId()).isEqualTo("MyStreamA");
            assertThat(entry.getEventNumber()).isEqualTo(0);
            assertThat(entry.getEventType()).isEqualTo("MyEvent");
            assertThat(entry.getEventId()).isEqualTo("8faef866-b80f-4952-9124-62819a6517aa");
            assertThat(entry.getDataContentType()).isEqualTo(
                    EnhancedMimeType.create("application/xml; encoding=utf-8"));
            assertThat(entry.getMetaContentType()).isEqualTo(
                    EnhancedMimeType.create("application/xml; encoding=utf-8"));
            assertThat(entry.getData()).isInstanceOf(JsonObject.class);
            assertThat(entry.getMeta()).isInstanceOf(JsonObject.class);

        } finally {
            in.close();
        }

    }

}
// CHECKSTYLE:ON
