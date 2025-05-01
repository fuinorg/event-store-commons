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

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.core.options.Constants.ANSI_COLORS_DISABLED_PROPERTY_NAME;
import static io.cucumber.core.options.Constants.FEATURES_PROPERTY_NAME;
import static io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.core.options.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Locates all cucumber files and starts them as unit tests.
 */
@Suite
@IncludeEngines("cucumber")
// @SelectClasspathResource("features") BUG! See https://github.com/serenity-bdd/serenity-cucumber-starter/issues/116
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "classpath:features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.fuin.esc.test")
@ConfigurationParameter(key = ANSI_COLORS_DISABLED_PROPERTY_NAME, value = "true")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,html:target/cucumber-html-report,summary")
public class CucumberMemIT {

    @BeforeSuite
    static void beforeSuite() {
        System.setProperty(TestUtils.IMPLEMENTATION_KEY, TestUtils.MEM_IMPLEMENTATION);
    }

    @AfterSuite
    static void afterSuite() {
        System.clearProperty(TestUtils.IMPLEMENTATION_KEY);
    }

}

