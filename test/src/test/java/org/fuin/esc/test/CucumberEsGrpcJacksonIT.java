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

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.ANSI_COLORS_DISABLED_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * Runs all cucumber features against the KurrentDB (esgrpc) backend using Jackson serialization. This is the
 * Jackson counterpart of {@link CucumberEsGrpcIT} (which uses JSON-B), so the same features are exercised
 * against both JSON serializers.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.fuin.esc.test")
@ConfigurationParameter(key = ANSI_COLORS_DISABLED_PROPERTY_NAME, value = "true")
public class CucumberEsGrpcJacksonIT {

    @BeforeSuite
    static void beforeSuite() {
        System.setProperty(TestUtils.IMPLEMENTATION_KEY, TestUtils.ESGRPC_JACKSON_IMPLEMENTATION);
    }

    @AfterSuite
    static void afterSuite() {
        System.clearProperty(TestUtils.IMPLEMENTATION_KEY);
    }

}
