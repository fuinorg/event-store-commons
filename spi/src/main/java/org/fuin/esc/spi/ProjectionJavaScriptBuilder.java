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

import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.TenantStreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.NotThreadSafe;
import org.fuin.utils4j.Utils4J;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the JavaScript for a 'fromCategory' projection.
 * <p>
 * Events can be selected by exact type name (see {@link #type(String)}) and/or by category (see
 * {@link #categories(List)}). A category is a tag carried in the event metadata (readable in the projection
 * as {@code ev.metadata.categories}) - typically the simple name of a marker interface the event implements.
 * When at least one category is used, a single {@code $any} handler with a combined type/category guard is
 * emitted (so an event is linked at most once); with types only, the original per-type handler map is kept.
 */
@NotThreadSafe
public final class ProjectionJavaScriptBuilder {

    private final boolean tenantProjection;

    @Nullable
    private final String tenantId;

    @Nullable
    private final String sourceCategory;

    private final String targetStream;

    private int count;

    private StringBuilder sb = new StringBuilder();

    private final List<String> typeList = new ArrayList<>();

    private final List<String> categoryList = new ArrayList<>();

    /**
     * Constructor for building a tenant or an 'all' projection.
     *
     * @param tenantId     Optional tenant identifier.
     * @param targetStreamId Identifier of the output stream the projection creates.
     */
    public ProjectionJavaScriptBuilder(@Nullable final TenantId tenantId,
                                       final StreamId targetStreamId) {
        super();
        Contract.requireArgNotNull("targetStreamId", targetStreamId);
        count = 0;
        this.sourceCategory = null;
        if (tenantId == null) {
            this.tenantId = null;
            tenantProjection = false;
            targetStream = new TenantStreamId(null, targetStreamId).getName();
            initAll();
        } else {
            this.tenantId = tenantId.asString();
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
    public ProjectionJavaScriptBuilder(final StreamId targetStreamId) {
        Contract.requireArgNotNull("targetStreamId", targetStreamId);
        count = 0;
        this.tenantId = null;
        this.sourceCategory = null;
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
    public ProjectionJavaScriptBuilder(final String categoryName,
                                       final StreamId targetStreamId) {
        super();
        Contract.requireArgNotNull("categoryName", categoryName);
        Contract.requireArgNotNull("targetStreamId", targetStreamId);
        count = 0;
        this.tenantId = null;
        this.sourceCategory = categoryName;
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
        typeList.add(eventType);
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
     * Adds another category (event metadata tag) to select.
     *
     * @param categoryName Category name to select.
     * @return this.
     */
    public ProjectionJavaScriptBuilder category(final String categoryName) {
        Contract.requireArgNotNull("categoryName", categoryName);
        categoryList.add(categoryName);
        return this;
    }

    /**
     * Adds more categories (event metadata tags) to select.
     *
     * @param categoryNames Category names to select.
     * @return this.
     */
    public ProjectionJavaScriptBuilder categories(final List<String> categoryNames) {
        Contract.requireArgNotNull("categoryNames", categoryNames);
        for (final String category : categoryNames) {
            category(category);
        }
        return this;
    }

    /**
     * Builds the JavaScript for the projection.
     *
     * @return Projection script.
     */
    public String build() {
        if (typeList.isEmpty() && categoryList.isEmpty()) {
            throw new IllegalStateException("No types were added. Use 'type(String)' to add at least one event.");
        }
        if (!categoryList.isEmpty()) {
            return buildWithCategories();
        }
        sb.append("""
                })
                """);
        return sb.toString();
    }

    /**
     * Builds the script when at least one category is selected: a single {@code $any} handler guarded by a
     * combined type/category check, so every selected event is linked exactly once regardless of whether it
     * matched by type name or by category.
     *
     * @return Projection script.
     */
    private String buildWithCategories() {
        final StringBuilder b = new StringBuilder();
        if (tenantProjection) {
            b.append(Utils4J.replaceVars("""
                    isTenant = (ev) => {
                      return (ev.metadata && ev.metadata.tenant && ev.metadata.tenant === "${tenantId}" );
                    }
                    """, Map.of("tenantId", tenantId)));
            b.append("\n");
        }
        b.append("hasCategory = (ev) => {\n");
        b.append("  return (ev.metadata && ev.metadata.categories && (");
        for (int i = 0; i < categoryList.size(); i++) {
            if (i > 0) {
                b.append(" || ");
            }
            b.append("ev.metadata.categories.indexOf('").append(categoryList.get(i)).append("') !== -1");
        }
        b.append("));\n");
        b.append("}\n\n");

        if (tenantProjection) {
            b.append(Utils4J.replaceVars("fromCategory('${tenantId}').foreachStream().when({\n",
                    Map.of("tenantId", tenantId)));
        } else if (sourceCategory != null) {
            b.append(Utils4J.replaceVars("fromCategory('${category}').foreachStream().when({\n",
                    Map.of("category", sourceCategory)));
        } else {
            b.append("fromAll().foreachStream().when({\n");
        }
        b.append("  $any: function(state, ev) {\n");
        b.append("    if (").append(guard()).append(") {\n");
        b.append(Utils4J.replaceVars("      linkTo('${targetStream}', ev);\n",
                Map.of("targetStream", targetStream)));
        b.append("    }\n");
        b.append("  }\n");
        b.append("})\n");
        return b.toString();
    }

    private String guard() {
        final String selection;
        if (typeList.isEmpty()) {
            selection = "hasCategory(ev)";
        } else {
            final StringBuilder types = new StringBuilder("[");
            for (int i = 0; i < typeList.size(); i++) {
                if (i > 0) {
                    types.append(",");
                }
                types.append("'").append(typeList.get(i)).append("'");
            }
            types.append("]");
            selection = "(" + types + ".indexOf(ev.eventType) !== -1 || hasCategory(ev))";
        }
        if (tenantProjection) {
            return "isTenant(ev) && " + selection;
        }
        return selection;
    }

}
