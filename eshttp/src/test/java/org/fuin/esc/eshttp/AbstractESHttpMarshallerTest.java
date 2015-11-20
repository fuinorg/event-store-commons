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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.activation.MimeTypeParseException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import org.apache.commons.io.IOUtils;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.SerializedData;
import org.fuin.esc.spi.SerializedDataType;

/**
 * Base class for {@link ESHttpMarshaller} implementation tests.
 */
// CHECKSTYLE:OFF Test
public abstract class AbstractESHttpMarshallerTest {

   
    protected final String createMyEventXml(final UUID uuid, final String description) {
        return "<my-event id=\"" + uuid + "\" description=\"" + description + "\" />";
    }

    protected final String createMyEventJson(final UUID uuid, final String descr) {
        return "{ \"my-event\": { \"id\":  \"" + uuid + "\", \"description\": \"" + descr + "\" } }";
    }

    protected final String createMyMetaXml() {
        return "<my-meta><user>abc</user></my-meta>";

    }
    
    protected final String createMyMetaJson() {
        return "{ \"my-meta\": { \"user\": \"abc\" } }";
    }

    protected final String loadJsonResource(final String name) throws IOException {
        final String json = IOUtils.toString(this.getClass().getResourceAsStream(name));
        final JsonObject js = Json.createReader(new StringReader(json)).readObject();
        final StringWriter sw = new StringWriter();
        createWriter(sw).writeObject(js);
        return sw.toString();
    }

    protected final JsonWriter createWriter(final Writer writer) {
        final Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        return Json.createWriterFactory(config).createWriter(writer);
    }

    protected final SerializedData asXml(final String type, final String mimeType, final String xml)
            throws MimeTypeParseException {
        final EnhancedMimeType emt = new EnhancedMimeType(mimeType);
        return new SerializedData(new SerializedDataType(type), new EnhancedMimeType(mimeType), xml.getBytes(emt.getEncoding()));
    }

    protected final SerializedData asJson(final String type, final String mimeType, final String json)
            throws MimeTypeParseException {

        final EnhancedMimeType emt = new EnhancedMimeType(mimeType);
        return new SerializedData(new SerializedDataType(type), emt, json.getBytes(emt.getEncoding()));
    }

}
// CHECKSTYLE:ON
