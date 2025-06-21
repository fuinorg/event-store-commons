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
package org.fuin.esc.spi;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.TenantStreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.utils4j.Utils4J;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Builds the JavaScript for a 'fromCategory' projection.
 */
public final class ProjectionJavaScriptBuilder {

    private final boolean tenantProjection;

    private final String targetStream;

    private int count;

    private StringBuilder sb;

    /**
     * Constructor for building a tenant or an 'all' projection.
     *
     * @param tenantId     Optional tenant identifier.
     * @param targetStreamId Identifier of the output stream the projection creates.
     */
    public ProjectionJavaScriptBuilder(@Nullable final TenantId tenantId,
                                       @NotNull final StreamId targetStreamId) {
        super();
        Contract.requireArgNotNull("targetStreamId", targetStreamId);
        count = 0;
        if (tenantId == null) {
            tenantProjection = false;
            targetStream = new TenantStreamId(null, targetStreamId).getName();
            initAll();
        } else {
            tenantProjection = true;
            targetStream = new TenantStreamId(tenantId, targetStreamId).getName();
            initTenant(tenantId);
        }
    }

    /**
     * Constructor for building an 'all' based projection.
     *
     * @param targetStreamId Identifier of the output stream the projection creates.
     */
    public ProjectionJavaScriptBuilder(@NotNull final StreamId targetStreamId) {
        Contract.requireArgNotNull("targetStreamId", targetStreamId);
        count = 0;
        tenantProjection = false;
        targetStream = new TenantStreamId(null, targetStreamId).getName();
        initAll();
    }


    /**
     * Constructor for building a 'category' based projection.
     *
     * @param categoryName Category name.
     * @param targetStreamId Identifier of the output stream the projection creates.
     */
    public ProjectionJavaScriptBuilder(@NotNull final String categoryName,
                                       @NotNull final StreamId targetStreamId) {
        super();
        Contract.requireArgNotNull("categoryName", categoryName);
        Contract.requireArgNotNull("targetStreamId", targetStreamId);
        count = 0;
        tenantProjection = false;
        targetStream = new TenantStreamId(null, targetStreamId).getName();
        initCategory(categoryName);
    }

    private void initAll() {
        sb = new StringBuilder();
        sb.append("""
                fromAll().foreachStream().when({
                """);
    }

    private void initCategory(final String category) {
        sb = new StringBuilder();
        sb.append(Utils4J.replaceVars("""
                fromCategory('${category}').foreachStream().when({
                """, Map.of("category", category)));
    }

    private void initTenant(TenantId tenantId) {
        sb = new StringBuilder();
        sb.append(Utils4J.replaceVars("""
                isTenant = (ev) => {
                  return (ev.metadata && ev.metadata.tenant && ev.metadata.tenant === "${tenantId}" );
                }
                
                fromCategory('${tenantId}').foreachStream().when({
                """, Map.of("tenantId", tenantId.asString())));
    }

    /**
     * Adds another type to select.
     *
     * @param eventType Unique event type to select from the category of streams.
     * @return this.
     */
    public ProjectionJavaScriptBuilder type(final String eventType) {
        if (count > 0) {
            sb.append(",");
        }
        if (tenantProjection) {
            sb.append(Utils4J.replaceVars("""
                      '${eventType}': function (state, ev) {
                         if (isTenant(ev)) {
                            linkTo('${targetStream}', ev);
                         }
                      }
                    """, Map.of("eventType", eventType,
                    "targetStream", targetStream)));
        } else {
            sb.append(Utils4J.replaceVars("""
                      '${eventType}': function(state, ev) {
                          linkTo('${targetStream}', ev);
                      }
                    """, Map.of("eventType", eventType,
                    "targetStream", targetStream)));
        }
        count++;
        return this;
    }

    /**
     * Adds another type to select. Convenience method to add a {@link TypeName} instead of a string.
     *
     * @param eventType Unique event type to select from the category of streams.
     * @return this.
     */
    public ProjectionJavaScriptBuilder type(final TypeName eventType) {
        return type(eventType.asBaseType());
    }

    /**
     * Adds more types to select. Convenience method to add multiple {@link TypeName} instead of strings.
     *
     * @param eventTypes Unique event type list to select from the category of streams.
     * @return this.
     */
    public ProjectionJavaScriptBuilder types(final List<TypeName> eventTypes) {
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
    public String build() {
        if (count == 0) {
            throw new IllegalStateException("No types were added. Use 'type(String)' to add at least one event.");
        }
        sb.append("""
                })
                """);
        return sb.toString();
    }

}
