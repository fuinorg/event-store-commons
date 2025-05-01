package org.fuin.esc.jpa;

import jakarta.json.bind.annotation.JsonbProperty;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.TypeName;

public class EventB {

    public static final String EVENT = "EventB";

    public static final TypeName TYPE = new TypeName(EVENT);

    public static final SerializedDataType SER_TYPE = new SerializedDataType(EVENT);

    @JsonbProperty
    private String b;

    protected EventB() {
    }

    public EventB(String b) {
        this.b = b;
    }

}
