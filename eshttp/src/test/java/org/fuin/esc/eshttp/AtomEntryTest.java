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

import java.nio.charset.Charset;

import javax.activation.MimeTypeParseException;

import org.fuin.esc.spi.EnhancedMimeType;
import org.junit.Test;

/**
 * Tests the {@link AtomEntry} class.
 */
// CHECKSTYLE:OFF Test
public class AtomEntryTest {

    @Test
    public void testCreate() throws MimeTypeParseException {

        // PREPARE
        String eventStreamId = "MyStreamA"; 
        int eventNumber = 1; 
        String dataType = "MyEvent"; 
        String eventId = "8faef866-b80f-4952-9124-62819a6517aa";
        EnhancedMimeType dataContentType = new EnhancedMimeType("application/xml; encoding=UTF-8");
        EnhancedMimeType metaContentType = new EnhancedMimeType("application/xml; encoding=ISO8859-1");
        String metaType = "MyMeta"; 
        String data = "<data/>"; 
        String meta = "<meta/>";
        
        // TEST
        final AtomEntry<String> testee = new AtomEntry<String>(eventStreamId, eventNumber, dataType, eventId,
                dataContentType, metaContentType, metaType, data, meta);


        // VERIFY
        assertThat(testee.getEventStreamId()).isEqualTo(eventStreamId);
        assertThat(testee.getEventNumber()).isEqualTo(eventNumber);
        assertThat(testee.getEventType()).isEqualTo(dataType);
        assertThat(testee.getEventId()).isEqualTo(eventId);
        assertThat(testee.getDataContentType()).isEqualTo(dataContentType);
        assertThat(testee.getDataContentType().getEncoding()).isEqualTo(Charset.forName("UTF-8"));
        assertThat(testee.getMetaContentType()).isEqualTo(metaContentType);
        assertThat(testee.getMetaContentType().getEncoding()).isEqualTo(Charset.forName("ISO-8859-1"));
        assertThat(testee.getData()).isEqualTo(data);
        assertThat(testee.getMeta()).isEqualTo(meta);

    }

}
// CHECKSTYLE:ON
