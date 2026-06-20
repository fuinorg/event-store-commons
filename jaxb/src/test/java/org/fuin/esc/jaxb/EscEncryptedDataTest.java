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
package org.fuin.esc.jaxb;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.fuin.utils4j.jaxb.MarshallerBuilder;
import org.fuin.utils4j.jaxb.UnmarshallerBuilder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.jaxb.JaxbUtils.marshal;
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

/**
 * Test for the {@link EscEncryptedData} JAXB binding.
 */
class EscEncryptedDataTest {

    @Test
    public void testMarshalUnmarshalRoundTrip() {

        // PREPARE
        final EscEncryptedData original = new EscEncryptedData("key-1", "1", "MyEvent",
                "application/json; encoding=UTF-8", "secret".getBytes(StandardCharsets.UTF_8));

        // TEST
        final Marshaller marshaller = new MarshallerBuilder().addClassesToBeBound(EscEncryptedData.class).build();
        final String xml = marshal(marshaller, original);
        final Unmarshaller unmarshaller = new UnmarshallerBuilder().addClassesToBeBound(EscEncryptedData.class).build();
        final EscEncryptedData copy = unmarshal(unmarshaller, xml);

        // VERIFY
        assertThat(xml).contains("key-id").contains("key-version").contains("data-type")
                .contains("content-type").contains("encrypted-data");
        assertThat(copy).isEqualTo(original);

    }

}
