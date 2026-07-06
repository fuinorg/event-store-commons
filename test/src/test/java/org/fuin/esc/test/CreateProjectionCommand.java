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
package org.fuin.esc.test;

import org.fuin.esc.api.ProjectionAdminEventStore;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.utils4j.TestCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates a projection that selects events by type name and/or by category. The projection name is also used
 * as the emitted projection stream name (read back with {@link ReadProjectionStreamCommand}).
 */
public final class CreateProjectionCommand implements TestCommand<TestContext> {

    // Creation (Initialized by Cucumber)

    private String name;

    private String enable;

    private String eventTypes;

    private String categories;

    // Initialization

    private ProjectionAdminEventStore admin;

    private ProjectionId projectionId;

    private ProjectionStreamId projectionStreamId;

    private boolean enabled;

    private List<TypeName> typeNames;

    private List<String> categoryNames;

    // Execution

    private Exception actualException;

    /**
     * Default constructor used by Cucumber.
     */
    public CreateProjectionCommand() {
        super();
    }

    /**
     * Creates an instance with values from the table headers in the feature.
     *
     * @param cucumberTable Column values.
     */
    public CreateProjectionCommand(final Map<String, String> cucumberTable) {
        this.name = cucumberTable.get("Name");
        this.enable = cucumberTable.get("Enable");
        this.eventTypes = cucumberTable.get("Event Types");
        this.categories = cucumberTable.get("Categories");
    }

    @Override
    public void init(final TestContext context) {
        this.admin = context.getProjectionAdmin();
        this.name = context.getCurrentEventStoreImplType() + "_" + name;
        this.projectionId = new ProjectionId(name);
        this.projectionStreamId = new ProjectionStreamId(name);
        final String enableValue = EscTestUtils.emptyAsNull(enable);
        this.enabled = (enableValue == null) || Boolean.parseBoolean(enableValue);
        this.typeNames = new ArrayList<>();
        for (final String type : split(eventTypes)) {
            typeNames.add(new TypeName(type));
        }
        // Prefix the selected categories with the implementation type to match the way AppendToStreamCommand
        // tags them, keeping the two esgrpc runs (JSON-B / Jackson) isolated on the shared KurrentDB.
        this.categoryNames = new ArrayList<>();
        for (final String category : split(categories)) {
            categoryNames.add(context.getCurrentEventStoreImplType() + "_" + category);
        }
    }

    private static List<String> split(final String commaSeparated) {
        final List<String> result = new ArrayList<>();
        final String value = EscTestUtils.emptyAsNull(commaSeparated);
        if (value != null) {
            for (final String part : value.split(",")) {
                result.add(part.trim());
            }
        }
        return result;
    }

    @Override
    public void execute() {
        try {
            admin.createProjection(projectionId, projectionStreamId, enabled, typeNames, categoryNames);
        } catch (final Exception ex) {
            this.actualException = ex;
        }
    }

    @Override
    public boolean isSuccessful() {
        return actualException == null;
    }

    @Override
    public String getFailureDescription() {
        return "[" + name + "] creating the projection failed: " + actualException;
    }

    @Override
    public void verify() {
        if (!isSuccessful()) {
            throw new RuntimeException(getFailureDescription(), actualException);
        }
    }

    @Override
    public String toString() {
        return "CreateProjectionCommand [name=" + name + ", enabled=" + enabled + ", types=" + eventTypes
                + ", categories=" + categories + ", actualException=" + actualException + "]";
    }

}
