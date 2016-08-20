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
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.utils4j.Utils4J;

import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

/**
 * Reads and JSON Atom feed.
 */
public final class AtomFeedJsonReader implements AtomFeedReader {

    @Override
    public final List<URI> readAtomFeed(final InputStream in) {

        final List<URI> uris = new ArrayList<>();

        final JsonReader jsonReader = Json.createReader(new InputStreamReader(in));
        final JsonObject jsonObj = jsonReader.readObject();
        final JSONArray read = JsonPath.read(jsonObj, "$.entries..id");
        if (read != null) {
            for (int i = 0; i < read.size(); i++) {
                final JsonString id = (JsonString) read.get(i);
                final String uri = id.getString();
                try {
                    uris.add(Utils4J.url(uri).toURI());
                } catch (final URISyntaxException ex) {
                    throw new RuntimeException("Couldn't create URI: " + uri);
                }
            }
        }

        return uris;

    }

    @Override
    public final CommonEvent readEvent(final DeserializerRegistry desRegistry, final InputStream in) {

        final AtomEntry<JsonValue> entry = readAtomEntry(in);

        final ESHttpJsonUnmarshaller unmarshaller = new ESHttpJsonUnmarshaller();

        final TypeName dataType = new TypeName(entry.getEventType());
        final Object data = unmarshaller.unmarshal(desRegistry, new SerializedDataType(entry.getEventType()),
                entry.getDataContentType(), entry.getData());
        final Object meta;
        final TypeName metaType;
        if (entry.getMetaType() == null) {
            metaType = null;
            meta = null;
        } else {
            metaType = new TypeName(entry.getMetaType());
            meta = unmarshaller.unmarshal(desRegistry, new SerializedDataType(entry.getMetaType()),
                    entry.getMetaContentType(), entry.getMeta());
        }

        return new SimpleCommonEvent(new EventId(entry.getEventId()), dataType, data, metaType, meta);

    }

    /**
     * Parses the atom data without creating the event itself from data &amp;
     * meta data.
     * 
     * @param in
     *            Input stream to read.
     * 
     * @return Entry.
     */
    public final AtomEntry<JsonValue> readAtomEntry(final InputStream in) {

        final JsonReader jsonReader = Json.createReader(new InputStreamReader(in));
        final JsonObject jsonObj = jsonReader.readObject();

        final String eventStreamId = ((JsonString) JsonPath.read(jsonObj, "$.content.eventStreamId")).getString();
        final int eventNumber = ((JsonNumber) JsonPath.read(jsonObj, "$.content.eventNumber")).intValue();
        final String eventType = ((JsonString) JsonPath.read(jsonObj, "$.content.eventType")).getString();
        final String eventId = ((JsonString) JsonPath.read(jsonObj, "$.content.eventId")).getString();

        final JsonObject escMetaObj = JsonPath.read(jsonObj, "$.content.metadata.esc-meta");

        final String dataContentTypeStr = escMetaObj.getString("data-content-type");
        final EnhancedMimeType dataContentType = EnhancedMimeType.create(dataContentTypeStr);
        final JsonValue data = JsonPath.read(jsonObj, "$.content.data");

        final EnhancedMimeType metaContentType;
        final String metaTypeStr;
        final JsonValue meta;
        if (escMetaObj.containsKey("meta-type")) {
            metaTypeStr = escMetaObj.getString("meta-type");
            final String metaContentTypeStr = escMetaObj.getString("meta-content-type");
            metaContentType = EnhancedMimeType.create(metaContentTypeStr);
            final String metaKey = extractMetaKey(escMetaObj);
            meta = escMetaObj.get(metaKey);
        } else {
            metaTypeStr = null;
            metaContentType = null;
            meta = null;
        }

        return new AtomEntry<JsonValue>(eventStreamId, eventNumber, eventType, eventId, dataContentType,
                metaContentType, metaTypeStr, data, meta);

    }

    private String extractMetaKey(final JsonObject escMetaObj) {
        final Set<String> keySet = escMetaObj.keySet();
        for (String key : keySet) {
            if (!(key.equals("data-type") || key.equals("data-content-type") || key.equals("meta-type")
                    || key.equals("meta-content-type"))) {
                return key;
            }
        }
        throw new IllegalStateException("Meta key not found in: " + marshal(escMetaObj));
    }

    private String marshal(final JsonObject jsonObj) {
        final StringWriter writer = new StringWriter();
        final JsonWriter jsonWriter = Json.createWriter(writer);
        try {
            jsonWriter.write(jsonObj);
        } finally {
            jsonWriter.close();
        }
        return writer.toString();
    }

}
