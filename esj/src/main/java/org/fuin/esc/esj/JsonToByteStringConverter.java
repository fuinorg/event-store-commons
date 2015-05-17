package org.fuin.esc.esj;

import javax.json.JsonObject;

import lt.emasina.esj.model.converter.ObjectToByteStringConverter;
import lt.emasina.esj.model.converter.StringToByteStringConverter;

import com.google.protobuf.ByteString;

/**
 * Converts a JSON object into protobuf's ByteString.
 */
public final class JsonToByteStringConverter implements
        ObjectToByteStringConverter<JsonObject> {

    private final StringToByteStringConverter delegate;

    /**
     * Default constructor.
     */
    public JsonToByteStringConverter() {
        super();
        delegate = new StringToByteStringConverter();
    }

    @Override
    public final int getContentType() {
        return JSON_DATA_TYPE;
    }

    @Override
    public final ByteString convert(final JsonObject obj) {
        return delegate.convert(obj);
    }

}
