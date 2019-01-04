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
package org.fuin.esc.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;

import javax.activation.MimeTypeParseException;
import javax.json.bind.annotation.JsonbProperty;

import org.eclipse.yasson.FieldAccessStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link JsonbDeSerializer} class.
 */
// CHECKSTYLE:OFF Test
public class JsonbDeSerializerTest {

    private static final SerializedDataType TYPE = new SerializedDataType("Person");
    
    private JsonbDeSerializer testee;

    @Before
    public void setup() throws MimeTypeParseException {
        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(TYPE, Person.class);
        
        testee = JsonbDeSerializer.builder()
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        testee.init(typeRegistry);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testMarshalUnmarshal() {

        // PREPARE
        final Person original = new Person("Peter", 21);

        // TEST
        final byte[] data = testee.marshal(original, TYPE);
        final Person copy = testee.unmarshal(data, TYPE, EnhancedMimeType.create("application/json; encoding=utf-8"));

        // VERIFY
        assertThat(copy.getName()).isEqualTo("Peter");
        assertThat(copy.getAge()).isEqualTo(21);

    }

    @Test
    public void testUnmarshalObject() {

        // PREPARE
        final Person peter = new Person("Peter", 21);

        // TEST
        final Person copy = testee.unmarshal(peter, TYPE, EnhancedMimeType.create("application/json; encoding=utf-8"));

        // VERIFY
        assertThat(copy).isSameAs(peter);
        assertThat(copy.getName()).isEqualTo("Peter");
        assertThat(copy.getAge()).isEqualTo(21);

    }
    
    public static class Person {
        
        @JsonbProperty("name")
        private String name;
        
        @JsonbProperty("age")
        private int age;

        protected Person() {
            super();
        }
        
        public Person(String name, int age) {
            super();
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

    }
    
}
// CHECKSTYLE:ON
