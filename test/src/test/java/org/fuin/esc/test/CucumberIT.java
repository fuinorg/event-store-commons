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

import cucumber.api.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Locates all cucumber files and starts them as unit tests.
 */

// @formatter:off
@RunWith(EscCucumber.class)
@EscCucumberArgs({ "jpa", "mem", "eshttp", "esjc", "esgrpc" })
@CucumberOptions(features = { "src/test/resources/features/" }, glue = { "org.fuin.esc.test" }, strict = true, monochrome = true, format = {
        "pretty", "html:target/cucumber-html-report",
        "junit:target/cucumber-junit-report/allcukes.xml" })
// @formatter:on
public class CucumberIT {

}

