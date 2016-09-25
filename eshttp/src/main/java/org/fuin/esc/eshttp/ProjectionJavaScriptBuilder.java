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
package org.fuin.esc.eshttp;

import java.util.List;

import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TypeName;

/**
 * Builds the JavaScript for a 'fromCategory' projection.
 */
public final class ProjectionJavaScriptBuilder {

    private int count;

    private String projection;

    private StringBuilder sb;

    /**
     * Constructor for building a 'category' based projection.
     * 
     * @param projectionId
     *            Projection name.
     * @param category
     *            Category.
     */
    public ProjectionJavaScriptBuilder(final StreamId projectionId, final StreamId category) {
        this(projectionId.getName(), category.getName());
    }

    /**
     * Constructor for building a 'category' based projection.
     * 
     * @param projection
     *            Projection name.
     * @param category
     *            Category.
     */
    public ProjectionJavaScriptBuilder(final String projection, final String category) {
        super();
        count = 0;
        this.projection = projection;
        sb = new StringBuilder();
        sb.append("fromCategory('" + category + "').foreachStream().when({");
    }

    /**
     * Adds another type to select.
     * 
     * @param eventType
     *            Unique event type to select from the category of streams.
     * 
     * @return this.
     */
    public final ProjectionJavaScriptBuilder type(final String eventType) {
        if (count > 0) {
            sb.append(",");
        }
        sb.append("'" + eventType + "': function(state, ev) { linkTo('" + projection + "', ev); }");
        count++;
        return this;
    }

    /**
     * Adds another type to select. Convenience method to add a {@link TypeName}
     * instead of a string.
     * 
     * @param eventType
     *            Unique event type to select from the category of streams.
     * 
     * @return this.
     */
    public final ProjectionJavaScriptBuilder type(final TypeName eventType) {
        return type(eventType.asBaseType());
    }

    /**
     * Adds more types to select. Convenience method to add multiple
     * {@link TypeName} instead of strings.
     * 
     * @param eventTypes
     *            Unique event type list to select from the category of streams.
     * 
     * @return this.
     */
    public final ProjectionJavaScriptBuilder types(final List<TypeName> eventTypes) {
        for (final TypeName type : eventTypes) {
            type(type.asBaseType());
        }
        return this;
    }

    /**
     * Builds the JavaScript for the projection.
     * 
     * @return Projection script.
     */
    public final String build() {
        if (count == 0) {
            throw new IllegalStateException(
                    "No types were added. Use 'type(String)' to add at least one event.");
        }
        sb.append("})");
        return sb.toString();
    }

}
