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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;

/**
 * A list of events.
 */
@XmlRootElement(name = EscEvents.EL_ROOT_NAME)
public final class EscEvents implements ToJsonCapable {

    /** Unique root element name of the type. */
    protected static final String EL_ROOT_NAME = "Events";

    private static final String EL_EVENT = "Event";

    /** Unique name of the type. */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /** Unique name of the serialized type. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    @XmlElement(name = EL_EVENT)
    private List<EscEvent> list;

    /**
     * Default constructor for JAXB.
     */
    protected EscEvents() {
        super();
    }

    /**
     * Constructor with array.
     * 
     * @param events
     *            Event array.
     */
    public EscEvents(@NotNull final EscEvent... events) {
        this(Arrays.asList(events));
    }

    /**
     * Constructor with list.
     * 
     * @param events
     *            Event list.
     */
    public EscEvents(@NotNull final List<EscEvent> events) {
        super();
        Contract.requireArgNotNull("events", events);
        this.list = events;
    }

    /**
     * Returns an immutable event list.
     * 
     * @return Unmodifiable list of events.
     */
    public final List<EscEvent> getList() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public final JsonArray toJson() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        for (final EscEvent event : list) {
            builder.add(event.toJson());
        }
        return builder.build();
    }

    /**
     * Creates in instance from the given JSON array.
     * 
     * @param jsonArray
     *            Array to read values from.
     * 
     * @return New instance.
     */
    public static EscEvents create(final JsonArray jsonArray) {
        final List<EscEvent> events = new ArrayList<>();
        for (final JsonValue jsonValue : jsonArray) {
            if (jsonValue.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException(
                        "All elements in the JSON array must be an JsonObject, but was: "
                                + jsonValue.getValueType());
            }
            events.add(EscEvent.create((JsonObject) jsonValue));
        }
        return new EscEvents(events);
    }

}
