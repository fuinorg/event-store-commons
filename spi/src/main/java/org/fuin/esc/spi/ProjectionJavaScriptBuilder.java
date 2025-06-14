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
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.TenantStreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.utils4j.Utils4J;

import java.util.List;
import java.util.Map;

/**
 * Builds the JavaScript for a 'fromCategory' projection.
 */
public final class ProjectionJavaScriptBuilder {

    private final boolean tenantProjection;

    private int count;

    private String projection;

    private StringBuilder sb;

    /**
     * Constructor for building a tenant based projection.
     *
     * @param tenantStreamId Tenant ID to use as category and delegate name as projection name.
     */
    public ProjectionJavaScriptBuilder(@NotNull final TenantStreamId tenantStreamId) {
        super();
        Contract.requireArgNotNull("tenantStreamId", tenantStreamId);
        tenantProjection = true;
        if (tenantStreamId.getTenantId() == null) {
            initAll(tenantStreamId.getDelegate().asString());
        } else {
            initTenant(tenantStreamId);
        }
    }

    /**
     * Constructor for building an 'all' based projection.
     *
     * @param projectionId Projection ID to use as projection name.
     */
    public ProjectionJavaScriptBuilder(@NotNull final StreamId projectionId) {
        super();
        tenantProjection = false;
        Contract.requireArgNotNull("projectionId", projectionId);
        initAll(projectionId.asString());
    }

    /**
     * Constructor for building an 'all' based projection.
     *
     * @param projection Projection name.
     */
    public ProjectionJavaScriptBuilder(@NotNull final String projection) {
        super();
        Contract.requireArgNotNull("projection", projection);
        tenantProjection = false;
        initAll(projection);
    }

    /**
     * Constructor for building a 'category' based projection.
     *
     * @param projectionId Projection.
     * @param categoryId   Category.
     */
    public ProjectionJavaScriptBuilder(@NotNull final StreamId projectionId,
                                       @NotNull final StreamId categoryId) {
        super();
        Contract.requireArgNotNull("projectionId", projectionId);
        Contract.requireArgNotNull("categoryId", categoryId);
        tenantProjection = false;
        initCategory(projectionId.asString(), categoryId.asString());
    }

    /**
     * Constructor for building a 'category' based projection.
     *
     * @param projection Projection name.
     * @param category   Category name.
     */
    public ProjectionJavaScriptBuilder(@NotNull final String projection,
                                       @NotNull final String category) {
        super();
        Contract.requireArgNotNull("projection", projection);
        Contract.requireArgNotNull("category", category);
        tenantProjection = false;
        initCategory(projection, category);
    }

    private void initAll(final String projection) {
        count = 0;
        this.projection = projection;
        sb = new StringBuilder();
        sb.append("""
                fromAll().foreachStream().when({
                """);
    }

    private void initCategory(final String projection, final String category) {
        count = 0;
        this.projection = projection;
        sb = new StringBuilder();
        sb.append(Utils4J.replaceVars("""
                fromCategory('${category}').foreachStream().when({
                """, Map.of("category", category)));
    }

    private void initTenant(TenantStreamId tenantStreamId) {
        count = 0;
        projection = tenantStreamId.getDelegate().asString();
        final String category = tenantStreamId.getTenantId().asString();

        sb = new StringBuilder();
        sb.append(Utils4J.replaceVars("""
                isTenant = (ev) => {
                  return (ev.meta && ev.meta.tenantId && ev.meta.tenantId === "${tenantId}" );
                }
                
                fromCategory('${category}').foreachStream().when({
                """, Map.of("tenantId", tenantStreamId.getTenantId().asString(),
                "category", category)));
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
                            linkTo('${projection}', ev);
                         }
                      }
                    """, Map.of("eventType", eventType, "projection", projection)));
        } else {
            sb.append(Utils4J.replaceVars("""
                      '${eventType}': function(state, ev) {
                          linkTo('${projection}', ev);
                      }
                    """, Map.of("eventType", eventType, "projection", projection)));
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
