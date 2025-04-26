package org.fuin.esc.jaxb;

import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.esc.jaxb.EscJaxbUtils.xmlDeSerializerBuilder;

/**
 * Test for the {@link EscJaxbUtils} class.
 */
class EscJaxbUtilsTest {

    @Test
    void testAddEscSerDeserializer() {

        final XmlDeSerializer xmlDeSer = xmlDeSerializerBuilder().build();
        final SimpleSerializerDeserializerRegistry.Builder builder = new SimpleSerializerDeserializerRegistry.Builder(EscJaxbUtils.MIME_TYPE);
        EscJaxbUtils.addEscSerDeserializer(builder, xmlDeSer);
        final SimpleSerializerDeserializerRegistry registry = builder.build();

        assertThat(registry.getSerializer(EscEvents.SER_TYPE)).isNotNull();
        assertThat(registry.getSerializer(EscEvent.SER_TYPE)).isNotNull();
        assertThat(registry.getSerializer(EscMeta.SER_TYPE)).isNotNull();
        assertThat(registry.getSerializer(Base64Data.SER_TYPE)).isNotNull();
        assertThat(registry.getDeserializer(EscEvents.SER_TYPE)).isNotNull();
        assertThat(registry.getDeserializer(EscEvent.SER_TYPE)).isNotNull();
        assertThat(registry.getDeserializer(EscMeta.SER_TYPE)).isNotNull();
        assertThat(registry.getDeserializer(Base64Data.SER_TYPE)).isNotNull();

    }


}