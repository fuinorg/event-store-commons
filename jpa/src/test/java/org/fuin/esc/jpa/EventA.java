package org.fuin.esc.jpa;

import jakarta.json.bind.annotation.JsonbProperty;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.TypeName;
import org.fuin.utils4j.TestOmitted;

@TestOmitted("This is only a test class")
public class EventA {

    public static final String EVENT = "EventA";

    public static final TypeName TYPE = new TypeName(EVENT);

    public static final SerializedDataType SER_TYPE = new SerializedDataType(EVENT);

    @JsonbProperty
    private String a;

    protected EventA() {
    }

    public EventA(String a) {
        this.a = a;
    }

}
