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
package org.fuin.esc.jsonb;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link EscJsonbUtils} class.
 */
@SuppressWarnings("java:S1186") // Methods should not be empty is fine here for the test
public class EscJsonbUtilsTest {

    @Test
    public void testJoinJsonbSerializers() {

        final JsonbSerializer<Object> a = new JsonbSerializer<Object>() {
            @Override
            public void serialize(Object obj, JsonGenerator generator, SerializationContext ctx) {
            }
        };
        final JsonbSerializer<Object> b = new JsonbSerializer<Object>() {
            @Override
            public void serialize(Object obj, JsonGenerator generator, SerializationContext ctx) {
            }
        };
        final JsonbSerializer<Object> c = new JsonbSerializer<Object>() {
            @Override
            public void serialize(Object obj, JsonGenerator generator, SerializationContext ctx) {
            }
        };
        final JsonbSerializer<Object> d = new JsonbSerializer<Object>() {
            @Override
            public void serialize(Object obj, JsonGenerator generator, SerializationContext ctx) {
            }
        };

        assertThat(EscJsonbUtils.joinJsonbSerializers(new JsonbSerializer<?>[]{})).isEmpty();
        assertThat(EscJsonbUtils.joinJsonbSerializers(new JsonbSerializer<?>[]{}, a)).containsExactly(a);
        assertThat(EscJsonbUtils.joinJsonbSerializers(new JsonbSerializer<?>[]{a}, b)).containsExactly(a, b);
        assertThat(EscJsonbUtils.joinJsonbSerializers(new JsonbSerializer<?>[]{a, b}, c)).containsExactly(a, b, c);
        assertThat(EscJsonbUtils.joinJsonbSerializers(new JsonbSerializer<?>[]{a, b}, c, d)).containsExactly(a, b, c, d);

    }

    @Test
    public void testJoinJsonbSerializerArrays() {

        final JsonbSerializer<Object> a = new JsonbSerializer<Object>() {
            @Override
            public void serialize(Object obj, JsonGenerator generator, SerializationContext ctx) {
            }
        };
        final JsonbSerializer<Object> b = new JsonbSerializer<Object>() {
            @Override
            public void serialize(Object obj, JsonGenerator generator, SerializationContext ctx) {
            }
        };
        final JsonbSerializer<Object> c = new JsonbSerializer<Object>() {
            @Override
            public void serialize(Object obj, JsonGenerator generator, SerializationContext ctx) {
            }
        };
        final JsonbSerializer<Object> d = new JsonbSerializer<Object>() {
            @Override
            public void serialize(Object obj, JsonGenerator generator, SerializationContext ctx) {
            }
        };

        assertThat(EscJsonbUtils.joinJsonbSerializerArrays(new JsonbSerializer<?>[]{})).isEmpty();
        assertThat(EscJsonbUtils.joinJsonbSerializerArrays(new JsonbSerializer<?>[]{}, new JsonbSerializer<?>[]{a})).containsExactly(a);
        assertThat(EscJsonbUtils.joinJsonbSerializerArrays(new JsonbSerializer<?>[]{a}, new JsonbSerializer<?>[]{b})).contains(a, b);
        assertThat(EscJsonbUtils.joinJsonbSerializerArrays(new JsonbSerializer<?>[]{a, b}, new JsonbSerializer<?>[]{c})).contains(a, b, c);
        assertThat(EscJsonbUtils.joinJsonbSerializerArrays(new JsonbSerializer<?>[]{a, b}, new JsonbSerializer<?>[]{c, d})).contains(a, b, c, d);
        assertThat(EscJsonbUtils.joinJsonbSerializerArrays(new JsonbSerializer<?>[]{a}, new JsonbSerializer<?>[]{b}, new JsonbSerializer<?>[]{c})).contains(a, b, c);

    }

    @Test
    public void testJoinJsonbDeserializers() {

        final JsonbDeserializer<Object> a = new JsonbDeserializer<Object>() {
            @Override
            public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
                return null;
            }
        };
        final JsonbDeserializer<Object> b = new JsonbDeserializer<Object>() {
            @Override
            public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
                return null;
            }
        };
        final JsonbDeserializer<Object> c = new JsonbDeserializer<Object>() {
            @Override
            public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
                return null;
            }
        };
        final JsonbDeserializer<Object> d = new JsonbDeserializer<Object>() {
            @Override
            public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
                return null;
            }
        };

        assertThat(EscJsonbUtils.joinJsonbDeserializers(new JsonbDeserializer<?>[]{})).isEmpty();
        assertThat(EscJsonbUtils.joinJsonbDeserializers(new JsonbDeserializer<?>[]{}, a)).containsExactly(a);
        assertThat(EscJsonbUtils.joinJsonbDeserializers(new JsonbDeserializer<?>[]{a}, b)).containsExactly(a, b);
        assertThat(EscJsonbUtils.joinJsonbDeserializers(new JsonbDeserializer<?>[]{a, b}, c)).containsExactly(a, b, c);
        assertThat(EscJsonbUtils.joinJsonbDeserializers(new JsonbDeserializer<?>[]{a, b}, c, d)).containsExactly(a, b, c, d);

    }

    @Test
    public void testJoinJsonbDeserializerArrays() {

        final JsonbDeserializer<Object> a = new JsonbDeserializer<Object>() {
            @Override
            public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
                return null;
            }
        };
        final JsonbDeserializer<Object> b = new JsonbDeserializer<Object>() {
            @Override
            public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
                return null;
            }
        };
        final JsonbDeserializer<Object> c = new JsonbDeserializer<Object>() {
            @Override
            public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
                return null;
            }
        };
        final JsonbDeserializer<Object> d = new JsonbDeserializer<Object>() {
            @Override
            public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
                return null;
            }
        };

        assertThat(EscJsonbUtils.joinJsonbDeserializerArrays(new JsonbDeserializer<?>[]{})).isEmpty();
        assertThat(EscJsonbUtils.joinJsonbDeserializerArrays(new JsonbDeserializer<?>[]{}, new JsonbDeserializer<?>[]{a})).containsExactly(a);
        assertThat(EscJsonbUtils.joinJsonbDeserializerArrays(new JsonbDeserializer<?>[]{a}, new JsonbDeserializer<?>[]{b})).contains(a, b);
        assertThat(EscJsonbUtils.joinJsonbDeserializerArrays(new JsonbDeserializer<?>[]{a, b}, new JsonbDeserializer<?>[]{c})).contains(a, b, c);
        assertThat(EscJsonbUtils.joinJsonbDeserializerArrays(new JsonbDeserializer<?>[]{a, b}, new JsonbDeserializer<?>[]{c, d})).contains(a, b, c, d);
        assertThat(EscJsonbUtils.joinJsonbDeserializerArrays(new JsonbDeserializer<?>[]{a}, new JsonbDeserializer<?>[]{b}, new JsonbDeserializer<?>[]{c})).contains(a, b, c);

    }

}

