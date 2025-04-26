/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.jackson;

import jakarta.activation.MimeTypeParseException;
import org.fuin.esc.api.EnhancedMimeType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link EscEvents} class.
 */
public class EscEventsTest extends AbstractTest {

    @Test
    public void testMarshalJackson() throws Exception {

        // PREPARE
        final String expectedJson = """
                [
                	{
                		"EventId": "b2a936ce-d479-414f-b67f-3df4da383d47",
                		"EventType": "MyEvent",
                		"Data": {
                            "id": "b2a936ce-d479-414f-b67f-3df4da383d47",
                			"description": "Hello, JSON!"
                		},
                		"MetaData": {
                			"data-type": "MyEvent",
                			"data-content-type": "application/json; version=1; encoding=UTF-8",
                			"meta-type": "MyMeta",
                			"meta-content-type": "application/json; version=1; encoding=UTF-8",
                			"MyMeta": {
                				"user": "abc"
                			}
                		}
                	},
                	{
                		"EventId": "68616d90-cf72-4c2a-b913-32bf6e6506ed",
                		"EventType": "MyEvent",
                		"Data": {
                			"Base64": "eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9"
                		},
                		"MetaData": {
                			"data-type": "MyEvent",
                			"data-content-type": "application/json; version=1; transfer-encoding=base64; encoding=UTF-8",
                			"meta-type": "MyMeta",
                			"meta-content-type": "application/json; version=1; encoding=UTF-8",
                			"MyMeta": {
                				"user": "abc"
                			}
                		}
                	}
                ]
                """;

        final EscEvents events = new EscEvents(createEvent1(), createEvent2());

        // TEST
        final String currentJson = getMapperProvider().writer().writeValueAsString(events);

        // VERIFY
        assertThatJson(currentJson).isEqualTo(expectedJson);

    }

    @Test
    public final void testUnmarshalJackson() throws Exception {

        // PREPARE
        final String json = """
                [
                	{
                		"EventId": "b2a936ce-d479-414f-b67f-3df4da383d47",
                		"EventType": "MyEvent",
                		"Data": {
                            "id": "b2a936ce-d479-414f-b67f-3df4da383d47",
                			"description": "Hello, JSON!"
                		},
                		"MetaData": {
                			"data-type": "MyEvent",
                			"data-content-type": "application/json; encoding=UTF-8",
                			"meta-type": "MyMeta",
                			"meta-content-type": "application/json; encoding=UTF-8",
                			"MyMeta": {
                				"user": "abc"
                			}
                		}
                	},
                	{
                		"EventId": "68616d90-cf72-4c2a-b913-32bf6e6506ed",
                		"EventType": "MyEvent",
                		"Data": {
                			"Base64": "eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9"
                		},
                		"MetaData": {
                			"data-type": "MyEvent",
                			"data-content-type": "application/json; transfer-encoding=base64; encoding=UTF-8",
                			"meta-type": "MyMeta",
                			"meta-content-type": "application/json; encoding=UTF-8",
                			"MyMeta": {
                				"user": "abc"
                			}
                		}
                	}
                ]
                """;

        // TEST
        final EscEvents testee = getMapperProvider().reader().readValue(json, EscEvents.class);

        // VERIFY
        assertThat(testee.getList()).hasSize(2);
        assertThat(testee.getList().get(0).getEventId()).isEqualTo("b2a936ce-d479-414f-b67f-3df4da383d47");
        assertThat(testee.getList().get(1).getEventId()).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");

    }

    private EscEvent createEvent1() throws MimeTypeParseException {
        final UUID eventId = UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47");
        final MyEvent myEvent = new MyEvent(UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("abc");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1");
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        return new EscEvent(eventId, MyEvent.TYPE.asBaseType(), new DataWrapper(myEvent), new DataWrapper(escMeta));
    }

    private EscEvent createEvent2() throws MimeTypeParseException {
        final UUID eventId = UUID.fromString("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        final Base64Data data = new Base64Data(
                "eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9");

        final MyMeta myMeta = new MyMeta("abc");

        final Map<String, String> params = new HashMap<>();
        params.put("transfer-encoding", "base64");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1", params);
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        return new EscEvent(eventId, MyEvent.TYPE.asBaseType(), new DataWrapper(data), new DataWrapper(escMeta));
    }

}

