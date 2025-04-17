package org.fuin.esc.test.jpa;

import org.fuin.esc.api.StreamId;
import org.fuin.esc.jpa.JpaIdStreamFactory;
import org.fuin.esc.jpa.JpaStream;
import org.fuin.esc.jpa.NoParamsStream;

/**
 * Creates a simple stream based on an identifer.
 */
public final class TestIdStreamFactory implements JpaIdStreamFactory {

    @Override
    public boolean containsType(final StreamId streamId) {
        // For test always true
        return true;
    }

    @Override
    public JpaStream createStream(final StreamId streamId) {
        // We only have no-arg streams in the test
        return new NoParamsStream(streamId);
    }

}

